
import os
import psutil
import sys
import time
import shutil

from datetime import datetime
from distutils.dir_util import copy_tree
from functools import wraps

from flask import Flask, render_template, session, redirect, url_for, request, jsonify, json

import versionCache
from customthreading import *

app = Flask(__name__)
app.config.from_object('config')

if app.debug:
	from werkzeug.debug import DebuggedApplication
	app.wsgi_app = DebuggedApplication(app.wsgi_app, True)

GAME_TYPES = {
	'Minecraft': {
		'port': 25500,
		'editorFiles': [
			['Banned-IPS', 'banned-ips.json'],
			['Banned-Players', 'banned-players.json'],
			['Ops', 'ops.json'],
			['Properties', 'server.properties'],
			['Whitelist', 'whitelist.json']
		],
		'runConfigKeys': [
			'ip',
			'other_java_params',
			'other_mc_params',
			'perm_gen',
			'ram_max',
			'ram_min',
			'version_forge',
			'version_minecraft'
		]
	},
	'Factorio': {
		'port': 0,
		'editorFiles': [],
		'runConfigKeys': []
	}
}
GAME_DATA = {
	'Minecraft': {
		'cache': versionCache.CacheMinecraft
	},
	'Factorio': {
		'cache': versionCache.Cache
	}
}

# ~~~~~~~~~~ Init: Config & Startup ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def getConfig(key):
	return app.config[key]

import models

def login_required(f):
	@wraps(f)
	def decorated_function(*args, **kwargs):
		if not 'username' in session:
			return getLoginURL()
		return f(*args, **kwargs)
	return decorated_function

def init():
	app.secret_key = os.urandom(20)
	versionCache.init(GAME_DATA, getConfig('RUN_DIRECTORY'))

# ~~~~~~~~~~ URL Redirects ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def getIndexURL():
	return redirect(url_for('index'))

def getLoginURL():
	return redirect(url_for('login'))

def getCurrentURL(formData):
	if 'current' in formData and formData['current'] != '':
		current = formData['current']
		url = None
		if current == "server":
			url = url_for(current, username = formData['nameOwner'], serverName = formData['nameServer'])
		else:
			url_for(current)
		return redirect(url)
	return getIndexURL()

def getUserURL(username):
	return redirect(url_for('user', username = username))

def getUserSettingsURL(username):
	return redirect(url_for('userSettings', username = username))

# ~~~~~~~~~~ Messages ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def setMessage(key, string):
	session[key] = string

def getMessage(key):
	string = None
	if key in session:
		string = session[key]
		session.pop(key, None)
	return string

def setError(string):
	setMessage('error', string)

def throwError(error, formData):
	setError(error)
	return getCurrentURL(formData)

def getError():
	return getMessage('error')

def setInfo(string):
	setMessage('info', string)

def getInfo():
	return getMessage('info')

# ~~~~~~~~~~ Rendering ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def renderPage(template, **kwargs):
	servers = []
	if isLoggedIn():
		for server in models.getServersForUser(session['username']):
			servers.append(server)
		for server in models.getServersForModerator(session['username']):
			if not server in servers:
				servers.append(server)
	return render_template(template,
			error = getError(),
			info = getInfo(),
			user_groups = models.getUserGroups(),
			server_types = GAME_TYPES,
			user_servers = servers,
			cache_versions = versionCache.cache_versions,
			**kwargs
		)

@app.errorhandler(404)
def not_found(error):
	return renderPage('pages/404.html'), 404

# ~~~~~~~~~~ Endpoints ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

@app.route('/', methods=['GET', 'POST'])
def index():
	return renderPage("pages/Homepage.html", current="index",
		modal = getMessage('modal'),
		runDirectory = getConfig('RUN_DIRECTORY')
	)

@app.route('/add/server', methods=['POST'])
@login_required
def addServer():
	name = request.form['name']
	port = request.form['port']
	game = request.form['type']
	
	username = session['username']
	user, error = models.getUser(username)
	print(user)
	server, error = models.getServer(name, username)
	
	if len(server) > 0:
		return throwError(request.form, 'Server with name "' + name + '" already exists for user "' + user.Username + '"')
	
	if partitionServer(name, port, game, request.form):
		models.Server.create(
				Name = name,
				User = user.Username,
				Port = port,
				Purpose = game
			)
		return redirect(url_for('server', username=username, serverName=name))
	return getCurrentURL(request.form)

@app.route('/add/server/admin', methods=['POST'])
@login_required
def addAdmin():
	ownerName = request.form['username']
	serverName = request.form['servername']
	user2Name = request.form['user']
	return addUserToServer(ownerName, serverName, user2Name, "Admin")

@app.route('/add/server/moderator', methods=['POST'])
@login_required
def addModerator():
	ownerName = request.form['username']
	serverName = request.form['servername']
	user2Name = request.form['user']
	return addUserToServer(ownerName, serverName, user2Name, "Moderator")

@app.route('/add/user', methods=['POST'])
@login_required
def addUser():
	username = request.form['username']
	password = generatePassword()
	group = request.form['group']
	if group == 'Other':
		group = request.form['group_other']
	return newUser(username, password, password, group, request.form)

@app.route('/add/user/new', methods=['POST'])
def createUser():
	username = request.form['username']
	password = request.form['password']
	ret = newUser(username, password, request.form['verify'], "User", request.form)
	
	error = loginUserPass(username, password)
	if error != None:
		setError(error)
		return openCreateAccount()
	return ret

@app.route('/changeUserPassword', methods=['POST'])
@login_required
def changeUserPassword():
	username = request.form['username']
	changePasswordForUser(
			username,
			request.form['old'],
			request.form['new'],
			request.form['verify']
		)
	return getUserSettingsURL(username)

@app.route('/changeUserPasswordAdmin', methods=['POST'])
@login_required
def changeUserPass_Admin():
	changePasswordForUser(
			request.form['username'],
			None,
			request.form['new'],
			request.form['verify']
		)
	setError(getMessage("errorPassword"))
	setInfo(getMessage("infoPassword"))
	return getCurrentURL(request.form)

@app.route('/delete/server', methods=['POST'])
@login_required
def deleteServer():
	data = request.form['data'].split('|')
	username = data[0]
	servername = data[1]
	
	models.deleteServerFromTable(servername, username)
	try:
		shutil.rmtree(getConfig('SERVERS_DIRECTORY') + username + "_" + servername + "/")
	except:
		pass
	
	return getIndexURL()

@app.route('/delete/user', methods=['POST'])
@login_required
def deleteUser():
	models.deleteUserFromTable(request.form['data'])
	return getCurrentURL(request.form)

@app.route('/servers')
@login_required
def servers():
	return renderPage('pages/TableServers.html', current = 'servers', data = models.getServers())

@app.route('/server/control/', methods=['POST'])
@login_required
def serverControl():
	form = request.form
	nameOwner = form['nameOwner']
	nameServer = form['nameServer']
	if 'start' in form:
		startServer(nameOwner, nameServer)
	elif 'stop' in form:
		stopServer(nameOwner, nameServer)
	elif 'kill' in form:
		killServer(nameOwner, nameServer)
	elif 'command' in form:
		sendCommandServer(nameOwner, nameServer, form['command'])
	return getCurrentURL(form)

@app.route('/server/updateFile', methods=['POST'])
@login_required
def updateServerFile():
	nameOwner = request.form['nameOwner']
	nameServer = request.form['nameServer']
	serverType = request.form['type']
	fileID = request.form['id']
	filename = request.form['filename']
	fileContent = request.form['content']
	
	serverDir = getDirectoryForServer(nameOwner, nameServer)
	filePath = os.path.join(serverDir, filename)
	with open(filePath, 'w') as f:
		f.write(fileContent)
	
	return redirect(url_for('server', username = nameOwner, serverName = nameServer))

@app.route('/server/saveRunConfig', methods=['POST'])
@login_required
def saveRunConfig():
	nameOwner = request.form['nameOwner']
	nameServer = request.form['nameServer']
	game = request.form['game']
	_saveRunConfig(nameOwner, nameServer, game, request.form)
	return redirect(url_for('server', username = nameOwner, serverName = nameServer))

@app.route('/settings/user')
@login_required
def userSettings():
	
	return renderPage('pages/UserSettings.html', username = session['username'],
			errorPassword = getMessage('errorPassword'),
			infoPassword = getMessage('infoPassword')
		)

@app.route('/users')
@login_required
def users():
	return renderPage('pages/TableUsers.html', current = 'users', data = models.getUsers())

@app.route('/user/<string:username>')
@login_required
def user(username):
	return renderPage('pages/UserProfile.html', current = None, username = username)

@app.route('/user/<string:username>/<string:serverName>')
@login_required
def server(username, serverName):
	serverModel, error = models.getServer(serverName, username)
	if serverModel != None:
		moderators, error = models.getModerators(serverName, username, permissions = "Moderator")
		admins, error = models.getModerators(serverName, username, permissions = "Admin")
		userIsAdmin = session['username'] == username or session['isAdmin'] or session['username'] in admins
		
		serverDirectory = getDirectoryForServer(username, serverName)
		
		editorFiles = GAME_TYPES[serverModel.Purpose]['editorFiles']
		fileData = []
		for fileDataArray in editorFiles:
			i = len(fileData)
			fileData.append({})
			fileData[i]['id'] = fileDataArray[0]
			filename = fileDataArray[1]
			fileData[i]['filename'] = filename
			with open(os.path.join(serverDirectory, filename), 'r') as f:
				fileData[i]['contents'] = f.read()
		
		directory = getConfig('SERVERS_DIRECTORY') + username + "_" + serverName + "/"
		with open(directory + "runConfig.json", 'r') as runJson:
			runConfig = json.load(runJson)
		
		return renderPage('pages/Server.html',
			current = "server",
			server = serverModel,
			moderators = moderators, admins = admins,
			userIsAdmin = userIsAdmin,
			fileData = fileData,
			runConfig = runConfig
		)
	else:
		return throwError(None, error)

@app.route('/_console', methods=['POST'])
def getConsole():
	serverName = request.form['serverName']
	ownerName = request.form['ownerName']
	
	consoleFile = getConfig('SERVERS_DIRECTORY') + ownerName + "_" + serverName + "/console.log"
	consoleText = ""
	if os.path.exists(consoleFile):
		with open(consoleFile) as f:
			consoleText = f.read()
	
	return jsonify(console = consoleText)

@app.route('/_onlinePlayers', methods=['GET', 'POST'])
def getOnlinePlayers():
	if request.method == 'GET':
		total = 0
		number = 0
	else:
		nameOwner = request.form['nameOwner']
		nameServer = request.form['nameServer']
		total = 0
		number = 0
	return jsonify(
		total = total,
		number = number
	)

@app.route('/_onlineServer', methods=['POST'])
def isServerOnline():
	nameOwner = request.form['nameOwner']
	nameServer = request.form['nameServer']
	isonline = False
	if nameOwner in activeServers:
		if nameServer in activeServers[nameOwner]:
			isonline = True
	return jsonify(online = isonline)

@app.route('/_onlineServers')
def getOnlineServers():
	
	totalServers = len(models.Server.select())
	
	totalOnline = 0
	for key in activeServers:
		totalOnline += len(activeServers[key])
	
	return jsonify(
			total = totalServers,
			number = totalOnline
		)

@app.route('/_onlineUsers')
def getOnlineUsers():
	return jsonify(
			total = len(models.User.select()),
			number = 0
		)

@app.route('/_ramUsage')
def getRamUsage():
	mem = psutil.virtual_memory()
	
	return jsonify(
			total = findBytes(mem.total),
			number = findBytes(mem.total - mem.available)
		)

@app.route('/_time')
def getTimeOnline():
	startTime = getConfig('SERVER_START')
	currentTime = int(round(time.time() * 1000))
	upTime = currentTime - startTime
	return jsonify(time = parse_minutes(upTime))

# ~~~~~~~~~~ Authentication ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

@app.route('/login', methods=['GET', 'POST'])
def login():
	if 'username' in session:
		return getIndexURL()
	elif request.method == 'POST':
		try:
			user = request.form['username']
			pw = request.form['password']
			valid, error = models.validatePassword(user, pw)
			if app.debug == True or valid == True:
				error = loginUserPass(user, pw)
				if error != None:
					setError(error)
					return openLogin()
			elif valid != True:
				setError(error)
				return openLogin()
			
			# Send user back to index page
			# (if username wasnt set, it will redirect back to login screen)
			return getIndexURL()
			
		except Exception as e:
			return throwError(str(e), request.form)
	else:
		return getIndexURL()

def loginUserPass(username, password):
	try:
		group = "Admin"
		if app.debug == False:
			user = models.User.select().where(models.User.Username == username).get()
			group = user.Group
		session['username'] = username
		session['displayName'] = username
		session['isAdmin'] = isAdmin(group)
		return None
	except Exception as e:
		return str(e)

def isAdmin(group):
	return group == "Admin"

def isLoggedIn():
	return 'username' in session

@app.route('/logout')
@login_required
def logout():
	session.pop('username', None)
	session.pop('displayName', None)
	session.pop('isAdmin', None)
	return getIndexURL()

# ~~~~~~~~~~ Lib ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def addUserToServer(nameOwner, nameServer, user2Name, permGroup):
	owner = None
	user2 = None
	
	error = None
	try:
		user, error = models.getUser(nameOwner)
		if error != None:
			setError('No user for owner with name "' + nameOwner + '"')
			return getIndexURL()
		else:
			owner = user
		
		user, error = None, None
		
		user, error = models.getUser(user2Name)
		if error != None:
			error = 'No user for moderator with name "' + user2Name + '"'
		else:
			user2 = user
	except Exception as e:
		error = str(e)
	
	if error != None:
		setError(error)
	else:
		try:
			models.Moderator.create(
				Owner = nameOwner,
				Server = nameServer,#models.getServer(nameOwner, nameServer),
				User = user2Name,
				Permissions = permGroup
			)
		except Exception as e:
			setError(str(e))
	
	return redirect(url_for('server', username=nameOwner, serverName=nameServer))

def changePasswordForUser(username, old, new, verify):
	
	user, error = models.getUser(username)
	
	error, info = None, None
	
	if old != None and old != user.Password:
		error = "Invalid Old Password"
	elif new != verify:
		error = "New Passwords Must Match"
	else:
		user.Password = new
		user.save()
		info = "Password Changed"
	
	setMessage('errorPassword', error)
	setMessage('infoPassword', info)

def findBytes(bytes):
	unit = pow(1000, 3)
	return str(bytes / unit) + "." + str(bytes % unit)[:2]

def generatePassword(length = 9, alpha = True, alphaUpper = True, numeric = True):
	import random
	alpha = "abcdefghijklmnopqrstuvwxyz"
	chars = ""
	if alpha:
		chars += alpha
	if alphaUpper:
		chars += alpha.upper()
	if numeric:
		chars += "0123456789"
	return ''.join(random.choice(chars) for _ in range(length))

def getDirectoryForServer(nameOwner, nameServer):
	servers_directory = getConfig('SERVERS_DIRECTORY')
	return servers_directory + nameOwner + "_" + nameServer + "/"

def openCreateAccount():
	return openModal('createUser')

def openModal(modal):
	setMessage('modal', modal)
	return getIndexURL()

def openLogin():
	return openModal('login')

def newUser(username, password, verify, group, formData):
	
	try:
		user, error = models.getUser(username)
		if len(user) > 0:
			return throwError("Username already in use: " + str(error), request.form)
	except Exception as e:
		return throwError(str(e), request.form)
	
	if password != verify:
		return throwError("Passwords do not match", formData)
	
	try:
		models.User.create(Username = username, Password = password, Group = group)
	except ValueError as e:
		return throwError(str(e), formData)
	except Exception as e:
		return throwError(str(e), formData)
	
	setInfo('The password for user "' + username + '" is "' + password + '"')
	return redirect(url_for('user', username=username))

def parse_minutes(time):
	milliseconds = int(time / 1000.0)
	time = milliseconds
	seconds = time % 60
	time = int(time / 60)
	minutes = time % 60
	time = int(time / 60)
	hours = time % 24
	time = int(time / 24)
	return str(time) + " days " + str(hours) + " hours " + str(minutes) + " minutes " + str(seconds) + " seconds"

def partitionServer(name, port, game, data):
	
	username = session['username']
	
	run_directory = getConfig('RUN_DIRECTORY')
	servers_directory = getConfig('SERVERS_DIRECTORY')
	directory = servers_directory + username + "_" + name + "/"
	
	if not os.path.exists(directory):
		os.makedirs(directory)
	
	if game == "Minecraft":
		
		# ~~~~~~~~~~ Copy Template
		
		copy_tree(run_directory + "templates/" + game + "/", directory, update = 1)
		
		# ~~~~~~~~~ Config
		
		_saveRunConfig(username, name, game, data)
		
		# ~~~~~~~~~~ Download server
		
		#copyServerMinecraft(directory,
		#	data['version_minecraft'], data['version_forge'])
		
		versionCache.downloadServerResources("Minecraft", directory, {
			'version': {
				'minecraft': data['version_minecraft'],
				'forge': data['version_forge']
			}
		})
		
		# ~~~~~~~~~~ End
		
		return True
	elif game == "Factorio":
		return True

def _saveRunConfig(nameOwner, nameServer, game, allData):
	directory = getConfig('SERVERS_DIRECTORY') + nameOwner + "_" + nameServer + "/"
	
	data = { key: allData[key] for key in GAME_TYPES[game]['runConfigKeys'] }
	
	with open(directory + "runConfig.json", 'w') as outfile:
		json.dump(data, outfile, sort_keys=True, indent=4, separators=(',', ': '))
	

# ~~~~~~~~~~ Active Servers ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

from serverManagement import Server

activeServers = {}

def getServer(nameOwner, nameServer):
	if nameOwner in activeServers and nameServer in activeServers[nameOwner]:
		return activeServers[nameOwner][nameServer]
	return None

def startServer(nameOwner, nameServer):
	if not nameOwner in activeServers:
		activeServers[nameOwner] = {}
	
	directory = getDirectoryForServer(nameOwner, nameServer)
	
	with open(directory + "runConfig.json", 'r') as outfile:
		activeServers[nameOwner][nameServer] = Server(nameOwner, nameServer, directory, json.load(outfile))
	
	getServer(nameOwner, nameServer).start()

def stopServer(nameOwner, nameServer):
	server = getServer(nameOwner, nameServer)
	if server != None: server.stop()

def killServer(nameOwner, nameServer):
	server = getServer(nameOwner, nameServer)
	if server != None: server.kill()

def sendCommandServer(nameOwner, nameServer, command):
	server = getServer(nameOwner, nameServer)
	if server != None: server.send(command)

# ~~~~~~~~~~ Run ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

if __name__ == '__main__':

    init()

    ctx = app.test_request_context()
    ctx.push()
    app.preprocess_request()
    port = int(os.getenv('PORT', 8085))
    host = os.getenv('IP', '0.0.0.0')
    app.config['SERVER_START'] = int(round(time.time() * 1000))
    app.run(port=port, host=host)

    models.db.connect()
    models.db.close()


