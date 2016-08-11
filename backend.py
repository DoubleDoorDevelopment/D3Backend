
import magic
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

from python.customthreading import *

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

from python.manageConfig import *

# ~~~~~~~~~~ Imports Post-Config ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

import database as Database

# ~~~~~~~~~~ Annotations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def login_required(f):
	@wraps(f)
	def decorated_function(*args, **kwargs):
		if not 'username' in session:
			return UrlData.getLoginURL()
		return f(*args, **kwargs)
	return decorated_function

# todo do @admin_required

# ~~~~~~~~~~ Inits ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

import python.servers as Servers

def init():
	app.secret_key = os.urandom(20)
	app.config['UPLOAD_FOLDER'] = getDirUploads()
	
	from python.Base import initDownloaders
	initDownloaders()
	Servers.initData()
	Servers.initCaches()
	Servers.initServers(Database.Server.select().order_by(Database.Server.User, Database.Server.Name))

# ~~~~~~~~~~ URL Getters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

import python.urls as UrlData

# ~~~~~~~~~~ Getter Session ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

import python.session as Session

# ~~~~~~~~~~ Rendering ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

def renderPage(template, **kwargs):
	userServers = []
	
	if Session.isLoggedIn():
		username = Session.getUsername()
		for serverModel in Database.getServersForUser(username):
			userServers.append(serverModel)
		for serverModel in Database.getServersForModerator(username):
			if not server in userServers:
				userServers.append(serverModel)
	
	return render_template(template,
			error = Session.getError(),
			info = Session.getInfo(),
			user_groups = Database.getUserGroups(),
			server_types = Servers.getServerData(),
			user_servers = userServers,
			caches = Servers.getServerCache(),
			**kwargs
		)

@app.errorhandler(404)
def not_found(error):
	return renderPage('pages/404.html'), 404

@app.template_filter('encodeText')
def filterFileEncode(value):
	try:
		return value.decode("utf-8")
	except Exception as e:
		return str(e)

# ~~~~~~~~~~ Endpoints ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

@app.route('/', methods=['GET', 'POST'])
def index():
	return renderPage("pages/Homepage.html", current="index",
		modal = Session.getMessage('modal')
	)

@app.route('/add/server', methods=['POST'])
@login_required
def addServer():
	name = request.form['name']
	game = request.form['type']
	
	print("Purpose ", game)
	
	username = Session.getUsername()
	server, error = Database.getServer(name, username)
	
	if len(server) > 0:
		return Session.throwError(request.form,
			'Server with name "' + name + '" already exists for user "' + username + '"')
	
	port, error = getNextPortForGame(game)
	if error != None:
		setError(error)
	else:
		Servers.createServerObj(username, name, game, port)
		
		if partitionServer(name, port, game, request.form, request.files):
			
			Database.Server.create(
				Name = name,
				User = username,
				Port = port,
				Purpose = game
			)
			
			return redirect(url_for('server', nameOwner = username, nameServer = name))
		else:
			removeServerObj(username, name)
	return Session.getCurrentURL(request.form)

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
	ret, error = newUser(username, password, password, group, request.form)
	if ret == None:
		return error
	else:
		return ret

@app.route('/add/user/new', methods=['POST'])
def createUser():
	username = request.form['username']
	password = request.form['password']
	if len(Database.getUsers()) > 0:
		group = "User"
	else:
		group = "Admin"
	ret, error = newUser(username, password, request.form['verify'], group, request.form)
	if ret == None:
		return error
	
	error = loginUserPass(username, password)
	if error != None:
		setError(error)
		return openCreateAccount()
	return ret

@app.route('/caches')
@login_required
def caches():
	
	return renderPage("pages/TableCaches.html", current = 'caches', data = Servers.getServerCache())

@app.route('/cache/<string:cacheName>/refresh')
@login_required
def refreshCache(cacheName):
	cache = None
	if cacheName.startswith("Minecraft"):
		cache = Servers.getServerCache()["Minecraft"]
		if cacheName == "Minecraft":
			cache.refreshManifestVanilla(True)
		else:
			cache.refreshManifestForge(True)
	else:
		cache = Servers.getServerCache()[cacheName]
		cache.refresh(force = True)
	
	return UrlData.getIndexURL()

@app.route('/changeUserPassword', methods=['POST'])
@login_required
def changeUserPassword():
	username = request.form['username']
	function = request.form['passwordType']
	
	if function == 'main':
		changePasswordForUser(
			username,
			request.form['old'],
			request.form['new'],
			request.form['verify']
		)
	elif function == 'steam':
		user, error = Database.getUser(username)
		user.setSteamCredentials(request.form['steamUser'], request.form['steamPass'])
		info = "Credentials Changed"
		Session.setMessage('errorPassword', error)
		Session.setMessage('infoPassword', info)
	
	return UrlData.getUserSettingsURL(username)

@app.route('/changeUserPasswordAdmin', methods=['POST'])
@login_required
def changeUserPass_Admin():
	changePasswordForUser(
			request.form['username'],
			None,
			request.form['new'],
			request.form['verify']
		)
	setError(Session.getMessage("errorPassword"))
	setInfo(Session.getMessage("infoPassword"))
	return getCurrentURL(request.form)

@app.route('/delete/server', methods=['POST'])
@login_required
def deleteServer():
	data = request.form['data'].split('|')
	nameOwner = data[0]
	nameServer = data[1]
	
	try:
		Database.deleteServerFromTable(nameOwner, nameServer)
		shutil.rmtree(getDirForServer(nameOwner, nameServer))
	except Exception as e:
		Session.setError(str(e))
		pass
	
	return UrlData.getIndexURL()

@app.route('/delete/user', methods=['POST'])
@login_required
def deleteUser():
	error = Database.deleteUserFromTable(request.form['data'])
	if error != None:
		Session.setError(error)
		return UrlData.getCurrentURL(request.form)
	else:
		return UrlData.getIndexURL()

@app.route('/servers')
@login_required
def servers():
	return renderPage('pages/TableServers.html', current = 'servers', data = Database.getServers())

@app.route('/server/control/', methods=['POST'])
@login_required
def serverControl():
	form = request.form
	nameOwner = form['nameOwner']
	nameServer = form['nameServer']
	
	server = Servers.getServer(nameOwner, nameServer)
	
	if 'start' in form:
		server.start()
	elif 'stop' in form:
		server.stop()
	elif 'kill' in form:
		server.kill()
	elif 'command' in form:
		server.send(form['command'])
	return UrlData.getCurrentURL(form)

@app.route('/shutdownServers')
@login_required
def shutdownServers():	
	servers = Servers.getServerObjs()
	
	for nameOwner in servers:
		for nameServer in servers[nameOwner]:
			if servers[nameOwner][nameServer].isOnline():
				servers[nameOwner][nameServer].stop()
	
	return UrlData.getIndexURL()

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
	
	server = getServer(nameOwner, nameServer)
	
	server.uploadAndInstallModpackFile(form, urlKey, files, filesKey, isCurse, app.config['UPLOAD_FOLDER'])
	
	return redirect(url_for('server', nameOwner = nameOwner, nameServer = nameServer))

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
	_saveRunConfig(nameOwner, nameServer, game, request.form, request.files)
	return redirect(url_for('server', nameOwner = nameOwner, nameServer = nameServer))

@app.route('/settings/user')
@login_required
def userSettings():
	username = Session.getUsername()
	user, error = Database.getUser(username)
	#steamUser, steamPass = user.getSteamCredentials()
	
	return renderPage('pages/UserSettings.html',
			username = username,
			steamUser = user.SteamUser,
			errorPassword = Session.getMessage('errorPassword'),
			infoPassword = Session.getMessage('infoPassword')
		)

@app.route('/users')
@login_required
def users():
	return renderPage('pages/TableUsers.html', current = 'users', data = Database.getUsers())

@app.route('/user/<string:username>')
@login_required
def user(username):
	return renderPage('pages/UserProfile.html', current = None, username = username)

@app.route('/user/<string:nameOwner>/<string:nameServer>')
@login_required
def server(nameOwner, nameServer):
	serverModel, error = Database.getServer(nameOwner, nameServer)
	if serverModel != None:
		moderators, error = Database.getModerators(nameServer, nameOwner, permissions = "Moderator")
		admins, error = Database.getModerators(nameServer, nameOwner, permissions = "Admin")
		
		username = Session.getUsername()
		userIsAdmin = username == nameOwner or session['isAdmin'] or username in admins
		
		directory = getDirForServer(nameOwner, nameServer)
		serverType = serverModel.Purpose
		serverData = Servers.getServerData(serverType)
		
		fileData = []
		for fileKey in serverData.getEdittableFileKeys():
			thisData = {}
			filename = serverData.getEdittableFile(fileKey)
			thisData['id'] = fileKey
			thisData['filename'] = filename
			with open(os.path.join(directory, filename), 'r') as f:
				thisData['contents'] = f.read()
			
			fileData.append(thisData)
		
		runConfig = {}
		if os.path.exists(directory + "run.json"):
			with open(directory + "run.json", 'r') as runJson:
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

@app.route('/user/<string:nameOwner>/<string:nameServer>/browser/', methods=['GET', 'POST'])
@app.route('/user/<string:nameOwner>/<string:nameServer>/browser/<path:path>', methods=['GET', 'POST'])
@login_required
def browseServer(nameOwner, nameServer, path = ''):
	serverModel, error = Database.getServer(nameOwner, nameServer)
	if serverModel != None:
		server = Servers.getServer(nameOwner, nameServer)
		filePath = server.dirRun + path

		exists = os.path.exists(filePath)
		isFile = False
		if exists:
			isFile = os.path.isfile(filePath)
		
		if request.method == 'POST':
			if 'function' in request.form:
				function = request.form['function']
				if function == 'saveFile':
					with open(filePath, 'w') as file:
						file.write(request.form['content'])
					return "Done"
				elif function == 'upload':
					file = request.files['file']
					if file:
						fileName = secure_filename(file.filename)
						pathForFile = filePath + "/" + fileName
						file.save(pathForFile)
				elif function.startswith("new"):
					fileDirName = request.form['name']
					fileDirPath = filePath + "/" + fileDirName
					newPath = path + "/" + fileDirName
					try:
						if function == "newFile":
							if not os.path.exists(fileDirPath):
								with open(fileDirPath, 'a') as file:
									file.write(" ");
						else:
							os.mkdir(fileDirPath)
						return redirect(url_for('browseServer', nameOwner = nameOwner, nameServer = nameServer, path = newPath))
					except Exception, e:
						Session.setError(str(e))
						print(str(e))
				elif function == 'delete':
					newPath = request.form['path']
					if exists:
						if isFile:
							os.remove(filePath)
						else:
							shutil.rmtree(filePath)
					return redirect(url_for('browseServer', nameOwner = nameOwner, nameServer = nameServer, path = newPath))
				elif function == 'command':
					elementPath = filePath + "/" + request.form['path']
					server.executeCommand(request.form['command'], filePath, elementPath)
		
		dirContents = {}
		fileContents = ""
		filename = 'none.txt'
		if not isFile:
			for item in os.listdir(filePath):
				itemPath = filePath + "/" + item
				itemIsFile = os.path.isfile(itemPath)
				itemIsBinary = False
				if itemIsFile:
					f = magic.Magic()
					mime = f.from_file(itemPath)
					itemIsBinary = not mime.startswith("ASCII")
				if itemIsFile:
					size = os.path.getsize(itemPath)
				else:
					size = '---'
				permissions = str(oct(os.stat(itemPath).st_mode & 0o777))[1:]
				dirContents[item] = {
					'isFile': itemIsFile,
					'isBinary': itemIsBinary,
					'size': size,
					'perms': permissions
				}
		else:
			filename = filePath.split('/')[-1]
			with open(filePath, 'r') as file:
				fileContents = file.read()
		
		isBinary = False
		if isFile:
			f = magic.Magic()
			mime = f.from_file(filePath)
			print(mime)
			isBinary = not (mime.startswith("ASCII") or mime == "very short file (no magic)")
			if isBinary:
				fileContents = mime
		
		return renderPage("/pages/ServerBrowser.html",
			server = serverModel,
			path = path,
			isFile = isFile, isBinary = isBinary,
			dirContents = dirContents,
			fileContents = fileContents,
			filename = filename
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
	if request.method == 'GET': # all servers
		total = 0
		number = 0
		for nameOwner in Servers.getServerObjs():
			for nameServer in Servers.getServers(nameOwner):
				server = Servers.getServer(nameOwner, nameServer)
				total += server.getPlayersOnline_Capacity()
				number += server.getPlayersOnline_Quantity()
	else:
		nameOwner = request.form['nameOwner']
		nameServer = request.form['nameServer']
		server = Servers.getServer(nameOwner, nameServer)
		total = server.getPlayersOnline_Capacity()
		number = server.getPlayersOnline_Quantity()
	return jsonify(
		total = total,
		number = number
	)

@app.route('/_onlineServer', methods=['POST'])
def isServerOnline():
	nameOwner = request.form['nameOwner']
	nameServer = request.form['nameServer']
	isonline = False
	if Servers.doesServerExist(nameOwner, nameServer):
		isonline = Servers.getServer(nameOwner, nameServer).isOnline()
	return jsonify(online = isonline)

@app.route('/_serverData', methods=['POST'])
def getServerMessageData():
	nameOwner = request.form['nameOwner']
	nameServer = request.form['nameServer']
	
	isDownloading = False
	errors = {}
	
	if Servers.doesServerExist(nameOwner, nameServer):
		server = Servers.getServer(nameOwner, nameServer)
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
	Servers.getServer(nameOwner, nameServer).removeError(category, int(index))
	return redirect(url_for('server', nameOwner = nameOwner, nameServer = nameServer))

@app.route('/_onlineServers')
def getOnlineServers():
	
	totalServers = len(Database.Server.select())
	
	totalOnline = 0
	for nameOwner in Servers.getServerObjs():
		for nameServer in Servers.getServers(nameOwner):
			if Servers.getServer(nameOwner, nameServer).isOnline():
				totalOnline += 1
	
	return jsonify(
			total = totalServers,
			number = totalOnline
		)

@app.route('/_onlineUsers')
def getOnlineUsers():
	return jsonify(
			total = len(Database.User.select()),
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
	if Session.isLoggedIn():
		return UrlData.getIndexURL()
	elif request.method == 'POST':
		#try:
		user = request.form['username']
		pw = request.form['password']
		valid, error = Database.validatePassword(user, pw)
		if getConfig("VALIDATE_LOGINS") == False or valid == True:
			error = loginUserPass(user, pw)
			if error != None:
				Session.setError(error)
				return openLogin()
		elif valid != True:
			Session.setError(error)
			return openLogin()
			
		Database.regenHash(user)
			
		# Send user back to index page
		# (if username wasnt set, it will redirect back to login screen)
		return UrlData.getIndexURL()
			
		#except Exception as e:
		#	print("exception #login")
		#0	return Session.throwError(str(e), request.form)
	else:
		return UrlData.getIndexURL()

@app.route('/logout')
@login_required
def logout():
	session.pop('username', None)
	session.pop('displayName', None)
	session.pop('isAdmin', None)
	return UrlData.getIndexURL()

def isAdmin(group):
	return group == "Admin"

def loginUserPass(username, password):
	try:
		group = "Admin"
		if getConfig("VALIDATE_ADMIN") == True:
			user = Database.User.select().where(Database.User.Username == username).get()
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
		user, error = Database.getUser(nameOwner)
		if error != None:
			setError('No user for owner with name "' + nameOwner + '"')
			return getIndexURL()
		else:
			owner = user
		
		user, error = None, None
		
		user, error = Database.getUser(user2Name)
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
			Database.Moderator.create(
				Owner = nameOwner,
				Server = nameServer,
				User = user2Name,
				Permissions = permGroup
			)
		except Exception as e:
			setError(str(e))
	
	return redirect(url_for('server', nameOwner = nameOwner, nameServer = nameServer))

def changePasswordForUser(username, old, new, verify):
	
	user, error = Database.getUser(username)
	
	error, info = None, None
	
	if old != None and old != user.Password:
		error = "Invalid Old Password"
	elif new != verify:
		error = "New Passwords Must Match"
	else:
		user.setPassword(new)
		user.save()
		info = "Password Changed"
	
	Session.setMessage('errorPassword', error)
	Session.setMessage('infoPassword', info)

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

def getNextPortForGame(game):
	query = Database.Server.select().where(Database.Server.Purpose == game)
	createdServers = len(query)
	data = Servers.getServerData(game)
	portMin, portMax = data.getPortRange()
	port = portMin + createdServers
	if port > portMax:
		return None, "Not enough ports for " + str(createdServers) + " servers for " + game
	else:
		return str(port), None

def openCreateAccount():
	return openModal('createUser')

def openModal(modal):
	Session.setMessage('modal', modal)
	return UrlData.getIndexURL()

def openLogin():
	return openModal('login')

def newUser(username, password, verify, group, formData):
	
	try:
		user, error = Database.getUser(username)
		if len(user) > 0:
			return (None, throwError("Username already in use: " + str(error), request.form))
	except Exception as e:
		return (None, throwError(str(e), request.form))
	
	if password != verify:
		return (None, throwError("Passwords do not match", formData))
	
	try:
		import safety
		salt = safety.randomString(50)
		passwordEncrypted = safety.encrypt(salt, password)
		#print("Encrypted Pass: '" + passwordEncrypted + "'")
		Database.User.create(
			Username = username,
			Password = passwordEncrypted,
			Salt = salt,
			SteamUser = "", SteamPass = "", SteamSalt = "",
			Group = group
		)
	except ValueError as e:
		return (None, Session.throwError(str(e), formData))
	except Exception as e:
		return (None, Session.throwError(str(e), formData))
	
	Session.setInfo('The password for user "' + username + '" is "' + password + '"')
	return (redirect(url_for('user', username=username)), None)

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

def partitionServer(name, port, game, data, files):
	
	username = Session.getUsername()
	directory = getDirForServer(username, name)
	
	if not os.path.exists(directory):
		os.makedirs(directory)
	
	# ~~~~~~~~~~ Copy Template
	
	copy_tree(getDirForTemplate(game), directory)
	
	try:
		server = Servers.getServer(username, name)
		server.setPort(port)
		_saveRunConfig(username, name, game, data, files)
	except Exception as e:
		Session.setError(str(e))
		print(str(e))
	
	return True

def _saveRunConfig(nameOwner, nameServer, game, allData, files):
	server = Servers.getServer(nameOwner, nameServer)
	user, error = Database.getUser(nameOwner)
	
	data = {}
	for key in Servers.getServerData(game).getRunConfigKeys():
		if key in allData:
			data[key] = allData[key]
	
	#import threading
	#threadInstall = threading.Thread(target=server.install, kwargs={'func': "server", 'data': data, 'files': files, 'user': user})
	#threadInstall.start()
	ThreadInstall(
		outputFilePath = os.path.join(server.dirRun, "console.log"),
		server = server,
		func = "server", data = data, files = files, user = user
	)

def testFunc():
	print("this is a test")

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

    Database.db.connect()
    Database.db.close()
