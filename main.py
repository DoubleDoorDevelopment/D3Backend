
import os
import psutil
import sys
import time
import shutil

from datetime import datetime
from distutils.dir_util import copy_tree
from functools import wraps
from threading import Thread

from flask import Flask, render_template, session, redirect, url_for, request, jsonify, json
from werkzeug import secure_filename

from customthreading import *

import lib.types as Types

app = Flask(__name__)

if app.debug:
	from werkzeug.debug import DebuggedApplication
	app.wsgi_app = DebuggedApplication(app.wsgi_app, True)

# ~~~~~~~~~~ Config ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

app.config.from_object('config')

def getConfig(key):
	if key in app.config:
		return app.config[key]
	else:
		return None

def getDirMain():
	return getConfig('MAIN_DIRECTORY')

def getDirStatic():
	return getDirMain() + "static/"

def getDirUploads():
	return getDirStatic() + "uploads/"

def getDirRun():
	return getConfig('RUN_DIRECTORY')

def getDirCache():
	return getDirRun() + "cache/"

def getDirServers():
	return getConfig('SERVERS_DIRECTORY')

def getDirForServer(nameOwner, nameServer):
	return getDirServers() + nameOwner + "_" + nameServer + "/"

def getDirForTemplate(game):
	return getDirRun() + "templates/" + game + "/"

# ~~~~~~~~~~ Imports Post-Config ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

import database

# ~~~~~~~~~~ Annotations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def login_required(f):
	@wraps(f)
	def decorated_function(*args, **kwargs):
		if not 'username' in session:
			return getLoginURL()
		return f(*args, **kwargs)
	return decorated_function

# ~~~~~~~~~~ Inits ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

servers = None
caches = None
serverData = None

def init():
	app.secret_key = os.urandom(20)
	app.config['UPLOAD_FOLDER'] = getDirUploads()
	
	initData()
	initCaches()
	initServers(database.Server.select().order_by(database.Server.User, database.Server.Name))
	from lib.Base import initDownloaders
	initDownloaders()

def initData():
	global serverData
	serverData = {}
	
	for key in Types.serverTypes:
		serverType = Types.serverTypes[key]
		serverData[serverType.getName()] = serverType.getDataClass()()

def initCaches():
	cacheDirectory = getDirCache()
	
	global caches
	caches = {}
	
	for serverTypeKey in Types.serverTypes:
		serverType = Types.serverTypes[serverTypeKey]
		caches[serverType.getName()] = serverType.getCacheClass()(cacheDirectory + serverType.getName() + "/")
		caches[serverType.getName()].refresh()

def initServers(serverQuery):
	global servers
	servers = {}
	
	for serverModel in serverQuery:
		createServerObj(serverModel.User.Username, serverModel.Name, serverModel.Purpose)

def getServerData(serverTypeName):
	return serverData[serverTypeName]

def doesServerExist(nameOwner, nameServer):
	return nameOwner in servers and nameServer in servers[nameOwner]

def getServer(nameOwner, nameServer):
	return servers[nameOwner][nameServer]

def createServerObj(nameOwner, nameServer, serverTypeName):
	global servers
	
	if not nameOwner in servers:
		servers[nameOwner] = {}
	
	directory = getDirForServer(nameOwner, nameServer)
	
	serverClass = Types.serverTypeToClass[serverTypeName]
	serverObj = serverClass(caches[serverTypeName], directory, nameOwner, nameServer)
	serverObj.setTypeServer(serverTypeName)
	servers[nameOwner][nameServer] = serverObj

def removeServerObj(nameOwner, nameServer):
	global servers
	if nameOwner in servers and nameServer in servers[nameOwner]:
		del servers[nameOwner][nameServer]

# ~~~~~~~~~~ URL Getters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def getIndexURL():
	return redirect(url_for('index'))

def getLoginURL():
	return redirect(url_for('login'))

def getCurrentURL(formData):
	if 'current' in formData and formData['current'] != '':
		current = formData['current']
		url = None
		if current == "server":
			url = url_for(current, nameOwner = formData['nameOwner'], nameServer = formData['nameServer'])
		else:
			url_for(current)
		return redirect(url)
	return getIndexURL()

def getUserURL(username):
	return redirect(url_for('user', username = username))

def getUserSettingsURL(username):
	return redirect(url_for('userSettings', username = username))

# ~~~~~~~~~~ Getter Session ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def isLoggedIn():
	return 'username' in session

def getUsername():
	return session['username']

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
	userServers = []
	
	if isLoggedIn():
		username = getUsername()
		for serverModel in database.getServersForUser(username):
			userServers.append(serverModel)
		for serverModel in database.getServersForModerator(username):
			if not server in userServers:
				userServers.append(serverModel)
	
	return render_template(template,
			error = getError(),
			info = getInfo(),
			user_groups = database.getUserGroups(),
			server_types = serverData,
			user_servers = userServers,
			caches = caches,
			refreshCacheOptions = ['Minecraft', 'Minecraft Forge', 'Factorio'],
			**kwargs
		)

@app.errorhandler(404)
def not_found(error):
	return renderPage('pages/404.html'), 404

# ~~~~~~~~~~ Endpoints ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

@app.route('/', methods=['GET', 'POST'])
def index():
	return renderPage("pages/Homepage.html", current="index",
		modal = getMessage('modal')
	)

@app.route('/add/server', methods=['POST'])
@login_required
def addServer():
	name = request.form['name']
	game = request.form['type']
	
	username = getUsername()
	server, error = database.getServer(name, username)
	
	if len(server) > 0:
		return throwError(request.form,
			'Server with name "' + name + '" already exists for user "' + username + '"')
	
	serverObj = createServerObj(username, name, game)
	
	if partitionServer(name, port, game, request.form):
		database.Server.create(
				Name = name,
				User = username,
				Port = "25500",
				Purpose = game
			)
		return redirect(url_for('server', nameOwner = username, nameServer = name))
	else:
		removeServerObj(username, name)
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

@app.route('/cache/<string:cacheName>/refresh')
@login_required
def refreshCache(cacheName):
	cache = None
	if cacheName.startswith("Minecraft"):
		cache = caches["Minecraft"]
		if cacheName == "Minecraft":
			cache.refreshManifestVanilla(True)
		else:
			cache.refreshManifestForge(True)
	else:
		cache = caches[cacheName]
		cache.refresh(force = True)
	
	return getIndexURL()

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
	nameOwner = data[0]
	nameServer = data[1]
	
	database.deleteServerFromTable(nameOwner, nameServer)
	try:
		shutil.rmtree(getDirForServer(nameOwner, nameServer))
	except Exception as e:
		setError(str(e))
		pass
	
	return getIndexURL()

@app.route('/delete/user', methods=['POST'])
@login_required
def deleteUser():
	error = database.deleteUserFromTable(request.form['data'])
	if error != None:
		setError(error)
	return getCurrentURL(request.form)

@app.route('/servers')
@login_required
def servers():
	return renderPage('pages/TableServers.html', current = 'servers', data = database.getServers())

@app.route('/server/control/', methods=['POST'])
@login_required
def serverControl():
	form = request.form
	nameOwner = form['nameOwner']
	nameServer = form['nameServer']
	
	server = getServer(nameOwner, nameServer)
	
	if 'start' in form:
		server.start()
	elif 'stop' in form:
		server.stop()
	elif 'kill' in form:
		server.kill()
	elif 'command' in form:
		server.send(form['command'])
	return getCurrentURL(form)

@app.route('/server/minecraft/uploadModpack/', methods=['POST'])
@login_required
def serverMinecraftUploadModpack():
	form = request.form
	files = request.files
	
	nameOwner = form['nameOwner']
	nameServer = form['nameServer']
	uploadType = form['type']
	isCurse = 'iscurse' in form
	
	urlKey, filesKey = None, None
	if uploadType == 'curseforge':
		pass
	elif uploadType == 'link':
		urlKey = 'link'
	elif uploadType == 'pack':
		filesKey = 'pack'
	
	fileLocation, error = uploadFile(form, urlKey, files, filesKey, nameOwner, nameServer, isCurse)
	if fileLocation != 0:
		if fileLocation == None:
			if error != None:
				setError("Unable to upload file")
			else:
				setError(error)
		else:
			installMinecraftModpack(fileLocation, nameOwner, nameServer, isCurse)
	
	return redirect(url_for('server', nameOwner = nameOwner, nameServer = nameServer))

def uploadFile(form, formURLKey, files, filesKey, nameOwner, nameServer, isCurse):
	if formURLKey != None:
		url = form[formURLKey]
		fileName = "modpack_" + nameOwner + "_" + nameServer + ".zip"
		filePath = os.path.join(app.config['UPLOAD_FOLDER'], fileName)
		Thread(
			target = uploadFileAndInstallPack,
			args = (url, fileName, filePath, nameOwner, nameServer, isCurse,)
		).start()
		return 0, None
	elif filesKey != None:
		file = files[filesKey]
		if file and isModpackFile(file.filename):
			fileName = secure_filename(file.filename)
			filePath = os.path.join(app.config['UPLOAD_FOLDER'], fileName)
			file.save(filePath)
			return filePath, None
		else:
			return None, "Invalid file"
	else:
		return None, None

def uploadFileAndInstallPack(url, fileName, filePath, nameOwner, nameServer, isCurse):
	import urllib
	urllib.urlretrieve(url, filePath)
	installMinecraftModpack(filePath, nameOwner, nameServer, isCurse)

def installMinecraftModpack(filePath, nameOwner, nameServer, isCurse):
	getServer(nameOwner, nameServer).install(func = 'modpack', filePath = filePath, isCurse = isCurse)

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
	return redirect(url_for('server', nameOwner = nameOwner, nameServer = nameServer))

@app.route('/settings/user')
@login_required
def userSettings():
	return renderPage('pages/UserSettings.html', username = getUsername(),
			errorPassword = getMessage('errorPassword'),
			infoPassword = getMessage('infoPassword')
		)

@app.route('/users')
@login_required
def users():
	return renderPage('pages/TableUsers.html', current = 'users', data = database.getUsers())

@app.route('/user/<string:username>')
@login_required
def user(username):
	return renderPage('pages/UserProfile.html', current = None, username = username)

@app.route('/user/<string:nameOwner>/<string:nameServer>')
@login_required
def server(nameOwner, nameServer):
	serverModel, error = database.getServer(nameOwner, nameServer)
	if serverModel != None:
		moderators, error = database.getModerators(nameServer, nameOwner, permissions = "Moderator")
		admins, error = database.getModerators(nameServer, nameOwner, permissions = "Admin")
		
		username = getUsername()
		userIsAdmin = username == nameOwner or session['isAdmin'] or username in admins
		
		directory = getDirForServer(nameOwner, nameServer)
		serverType = serverModel.Purpose
		serverData = getServerData(serverType)
		
		fileData = []
		for fileKey in serverData.getEdittableFileKeys():
			thisData = {}
			filename = serverData.getEdittableFile(fileKey)
			thisData['id'] = fileKey
			thisData['filename'] = filename
			with open(os.path.join(directory, filename), 'r') as f:
				thisData['contents'] = f.read()
			
			fileData.append(thisData)
		
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
	nameServer = request.form['serverName']
	nameOwner = request.form['ownerName']
	
	consoleFile = getConfig('SERVERS_DIRECTORY') + nameOwner + "_" + nameServer + "/console.log"
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
	if doesServerExist(nameOwner, nameServer):
		isonline = getServer(nameOwner, nameServer).isOnline()
	return jsonify(online = isonline)

@app.route('/_serverData', methods=['POST'])
def getServerMessageData():
	nameOwner = request.form['nameOwner']
	nameServer = request.form['nameServer']
	
	isDownloading = False
	errors = {}
	
	if doesServerExist(nameOwner, nameServer):
		server = getServer(nameOwner, nameServer)
		isDownloading = server.isDownloading()
		errors = server.getErrors()

	return jsonify(isDownloading = isDownloading, errors = errors)

@app.route('/_serverData/removeNotification', methods=['POST'])
@login_required
def removeServerNotification():
	form = request.form;
	nameOwner = form['nameOwner']
	nameServer = form['nameServer']
	category = form['category']
	index = form['index']
	getServer(nameOwner, nameServer).removeError(category, int(index))
	return redirect(url_for('server', nameOwner = nameOwner, nameServer = nameServer))

@app.route('/_onlineServers')
def getOnlineServers():
	
	totalServers = len(database.Server.select())
	
	totalOnline = 0
	for key in servers:
		totalOnline += len(servers[key])
	
	return jsonify(
			total = totalServers,
			number = totalOnline
		)

@app.route('/_onlineUsers')
def getOnlineUsers():
	return jsonify(
			total = len(database.User.select()),
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
	if isLoggedIn():
		return getIndexURL()
	elif request.method == 'POST':
		try:
			user = request.form['username']
			pw = request.form['password']
			valid, error = database.validatePassword(user, pw)
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

@app.route('/logout')
@login_required
def logout():
	session.pop('username', None)
	session.pop('displayName', None)
	session.pop('isAdmin', None)
	return getIndexURL()

def isAdmin(group):
	return group == "Admin"

def loginUserPass(username, password):
	try:
		group = "Admin"
		if app.debug == False:
			user = database.User.select().where(database.User.Username == username).get()
			group = user.Group
		session['username'] = username
		session['displayName'] = username
		session['isAdmin'] = isAdmin(group)
		return None
	except Exception as e:
		return str(e)

# ~~~~~~~~~~ Lib ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def addUserToServer(nameOwner, nameServer, user2Name, permGroup):
	owner = None
	user2 = None
	
	error = None
	try:
		user, error = database.getUser(nameOwner)
		if error != None:
			setError('No user for owner with name "' + nameOwner + '"')
			return getIndexURL()
		else:
			owner = user
		
		user, error = None, None
		
		user, error = database.getUser(user2Name)
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
			database.Moderator.create(
				Owner = nameOwner,
				Server = nameServer,
				User = user2Name,
				Permissions = permGroup
			)
		except Exception as e:
			setError(str(e))
	
	return redirect(url_for('server', nameOwner = nameOwner, nameServer = nameServer))

def changePasswordForUser(username, old, new, verify):
	
	user, error = database.getUser(username)
	
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

def isModpackFile(filename):
	return '.' in filename and \
			filename.rsplit('.', 1)[1] in ['zip']

def openCreateAccount():
	return openModal('createUser')

def openModal(modal):
	setMessage('modal', modal)
	return getIndexURL()

def openLogin():
	return openModal('login')

def newUser(username, password, verify, group, formData):
	
	try:
		user, error = database.getUser(username)
		if len(user) > 0:
			return throwError("Username already in use: " + str(error), request.form)
	except Exception as e:
		return throwError(str(e), request.form)
	
	if password != verify:
		return throwError("Passwords do not match", formData)
	
	try:
		database.User.create(Username = username, Password = password, Group = group)
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
	
	username = getUsername()
	directory = getDirForServer(username, name)
	
	if not os.path.exists(directory):
		os.makedirs(directory)
	
	if game == "Minecraft":
		
		# ~~~~~~~~~~ Copy Template
		
		copy_tree(getDirForTemplate(game), directory, update = 1)
		
		# ~~~~~~~~~ Config
		
		_saveRunConfig(username, name, game, data)
		
		# ~~~~~~~~~~ Download server
		
		
		
		# ~~~~~~~~~~ End
		
		return True
	elif game == "Factorio":
		return True

def _saveRunConfig(nameOwner, nameServer, game, allData):
	directory = getDirForServer(nameOwner, nameServer)
	
	data = { key: allData[key] for key in serverData[game].getRunConfigKeys() }
	
	with open(directory + "runConfig.json", 'w') as outfile:
		json.dump(data, outfile, sort_keys=True, indent=4, separators=(',', ': '))
	
	server = getServer(nameOwner, nameServer)
	server.install(func = "jars", mc = data['version_minecraft'], forge = data['version_forge'])

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

    database.db.connect()
    database.db.close()