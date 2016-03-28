
import os

from datetime import datetime
from flask import Flask, render_template, session, redirect, url_for, request, jsonify, json
from functools import wraps
import sys
import psutil
import time
from distutils.dir_util import copy_tree
import shutil
from customthreading import *
import versionCache

app = Flask(__name__)
app.config.from_object('config')
GAME_TYPES = {
	'Minecraft': { 'port': 25500, },
	'Factorio': { 'port': 0 }
}
GAME_DATA = {
	'Minecraft': { 'cache': versionCache.CacheMinecraft },
	'Factorio': { 'cache': versionCache.Cache }
}

thread_downloaders_pool = None
thread_downloaders_master = None

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
	initThreads()
	versionCache.init(GAME_DATA, getConfig('RUN_DIRECTORY'))

def initThreads():
	initDownloading()

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

# ~~~~~~~~~~ Downloading ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def initDownloading():
	global thread_downloaders_pool
	global thread_downloaders_master
	thread_downloaders_pool = ThreadPool(4)
	thread_downloaders_master = ThreadMaster(thread_downloaders_pool)
	thread_downloaders_master.start()
	print(thread_downloaders_master)

def addDownload(url, file):
	thread_downloaders_master.addDownload(url, file)

def addDownloadFunc(func, url, *args, **kargs):
	thread_downloaders_master.addDownloadFunc(func, url, *args, **kargs)

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

# ~~~~~~~~~~ Pages ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

@app.route('/', methods=['GET', 'POST'])
def index():
	return renderPage("pages/Homepage.html", current="index",
		modal = getMessage('modal'),
		runDirectory = getConfig('RUN_DIRECTORY')
	)

@app.route('/user/<string:username>')
@login_required
def user(username):
	return renderPage('pages/UserProfile.html', current = None, username = username)

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

@app.route('/servers')
@login_required
def servers():
	return renderPage('pages/TableServers.html', current = 'servers', data = models.getServers())

@app.route('/user/<string:username>/<string:serverName>')
@login_required
def server(username, serverName):
	server, error = models.getServer(serverName, username)
	if server != None:
		moderators, error = models.getModerators(serverName, username, permissions = "Moderator")
		admins, error = models.getModerators(serverName, username, permissions = "Admin")
		userIsAdmin = session['username'] == username or session['isAdmin'] or session['username'] in admins
		return renderPage('pages/Server.html', server = server, moderators = moderators, admins = admins, userIsAdmin = userIsAdmin, current = "server")
	else:
		return throwError(None, error)

def openModal(modal):
	setMessage('modal', modal)
	return getIndexURL()

def openLogin():
	return openModal('login')

def openCreateAccount():
	return openModal('createUser')

# ~~~~~~~~~~ Forms ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
		
		with open(directory + "runConfig.json", 'w') as outfile:
			json.dump(data, outfile, sort_keys=True, indent=4, separators=(',', ': '))
		
		# ~~~~~~~~~~ Download server
		
		#copyServerMinecraft(directory,
		#	data['version_minecraft'], data['version_forge'])
		
		# ~~~~~~~~~~ End
		
		return True
	elif game == "Factorio":
		return True

def copyServerMinecraft(directory, versionMinecraft, versionForge):
	directory_cache = getConfig('RUN_DIRECTORY') + "cache/"
	directory_versions_minecraft = directory_cache + "versions/Minecraft/"
	directory_vanilla = directory_versions_minecraft + "vanilla/"
	
	for file in os.listdir(directory):
		if str(file).endswith(".jar"):
			os.remove(directory + file)
	
	serverFileMinecraft = directory_vanilla + versionMinecraft + ".jar"
	shutil.copyfile(serverFileMinecraft, directory + "minecraft_server." + versionMinecraft + ".jar")
	
	if versionForge != '':
		directory_forge = directory_versions_minecraft + "forge/"
		jarFile = versionMinecraft + "-" + versionForge + ".jar"
		serverFileForge = directory_forge + jarFile
		shutil.copyfile(serverFileForge, directory + "forge-" + jarFile)

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

@app.route('/add/user', methods=['POST'])
@login_required
def addUser():
	username = request.form['username']
	password = generatePassword()
	group = request.form['group']
	if group == 'Other':
		group = request.form['group_other']
	return newUser(username, password, password, group, request.form)

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

@app.route('/delete/user', methods=['POST'])
@login_required
def deleteUser():
	models.deleteUserFromTable(request.form['data'])
	return getCurrentURL(request.form)

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

@app.route('/add/server/moderator', methods=['POST'])
@login_required
def addModerator():
	ownerName = request.form['username']
	serverName = request.form['servername']
	user2Name = request.form['user']
	return addUserToServer(ownerName, serverName, user2Name, "Moderator")

@app.route('/add/server/admin', methods=['POST'])
@login_required
def addAdmin():
	print()

def addUserToServer(ownerName, serverName, user2Name, permGroup):
	owner = None
	user2 = None
	
	error = None
	try:
		user, error = models.getUser(ownerName)
		if error != None:
			setError('No user for owner with name "' + ownerName + '"')
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
				Owner = owner,
				Server = models.getServer(serverName, ownerName),
				User = user2,
				Permissions = permGroup
			)
		except Exception as e:
			setError(str(e))
	
	return redirect(url_for('server', username=ownerName, serverName=serverName))

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
	return getCurrentURL(form)

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

# ~~~~~~~~~~ Active Servers ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

from serverManagement import Server

activeServers = {}

def getServer(nameOwner, nameServer):
	return activeServers[nameOwner][nameServer]

def startServer(nameOwner, nameServer):
	if not nameOwner in activeServers:
		activeServers[nameOwner] = {}
	
	servers_directory = getConfig('SERVERS_DIRECTORY')
	directory = servers_directory + nameOwner + "_" + nameServer + "/"
	
	with open(directory + "runConfig.json", 'r') as outfile:
		activeServers[nameOwner][nameServer] = Server(nameOwner, nameServer, directory, json.load(outfile))
	getServer(nameOwner, nameServer).start()

def stopServer(nameOwner, nameServer):
	getServer(nameOwner, nameServer).stop()

def killServer(nameOwner, nameServer):
	getServer(nameOwner, nameServer).kill()

# ~~~~~~~~~~ Javascript Ajax ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

@app.route('/_time')
def getTimeOnline():
	startTime = getConfig('SERVER_START')
	currentTime = int(round(time.time() * 1000))
	upTime = currentTime - startTime
	return jsonify(time = parse_minutes(upTime))

@app.route('/_onlinePlayers')
def getOnlinePlayers():
	return jsonify(
			total = 50,
			number = 28
		)

@app.route('/_ramUsage')
def getRamUsage():
	mem = psutil.virtual_memory()
	
	return jsonify(
			total = findBytes(mem.total),
			number = findBytes(mem.total - mem.available)
		)

@app.route('/_onlineServers')
def getOnlineServers():
	return jsonify(
			total = 4,
			number = 3
		)

@app.route('/_onlineUsers')
def getOnlineUsers():
	return jsonify(
			total = 20,
			number = 1
		)

@app.route('/_console', methods=['POST'])
def getConsole():
	serverName = request.form['serverName']
	ownerName = request.form['ownerName']
	
	consoleFile = getConfig('SERVERS_DIRECTORY') + serverName + "/console.log"
	consoleText = ""
	if os.path.exists(consoleFile):
		with open(consoleFile) as f:
			consoleText = f.read()
	
	return jsonify(console = consoleText)

# ~~~~~~~~~~ Other ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

@app.errorhandler(404)
def not_found(error):
	return renderPage('pages/404.html'), 404

# ~~~~~~~~~~ Lib ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def findBytes(bytes):
	unit = pow(1000, 3)
	return str(bytes / unit) + "." + str(bytes % unit)[:2]

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

# ~~~~~~~~~~ Run ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

if __name__ == '__main__':

	init()
	
	ctx = app.test_request_context()
	ctx.push()
	app.preprocess_request()
	port = int(os.getenv('PORT', 8080))
	host = os.getenv('IP', '0.0.0.0')
	app.config['SERVER_START'] = int(round(time.time() * 1000))
	app.run(port=port, host=host)
	
	models.db.connect()
	models.db.close()
