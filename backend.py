
import os

from datetime import datetime
from flask import Flask, render_template, session, redirect, url_for, request, jsonify
from functools import wraps
import sys
import psutil
import time
from distutils.dir_util import copy_tree
import shutil

app = Flask(__name__)
app.config.from_object('config')
GAME_TYPES = {
		'Minecraft': { 'port': 25500 },
		'Factorio': { 'port': 0 }
	}

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

def getIndexURL():
	return redirect(url_for('index'))

def getLoginURL():
	return redirect(url_for('login'))

def getCurrentURL(formData):
	if 'current' in formData and formData['current'] != '':
		return redirect(url_for(formData['current']))
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
		return renderPage('pages/Server.html', server = server, moderators = moderators, admins = admins, userIsAdmin = userIsAdmin)
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
	
	user, error = models.getUser(session['username'])
	server, error = models.getServer(name, user.Username)
	
	if len(server) > 0:
		return throwError(request.form, 'Server with name "' + name + '" already exists for user "' + user.Username + '"')
	
	if partitionServer(name, port, game, request.form):
		models.Server.create(
				Name = name,
				User = user.Username,
				Port = port,
				Purpose = game
			)
	return getCurrentURL(request.form)

def partitionServer(name, port, game, data):
	
	run_directory = getConfig('RUN_DIRECTORY')
	servers_directory = getConfig('SERVERS_DIRECTORY')
	directory = servers_directory + name + "/"
	
	if not os.path.exists(directory):
		os.makedirs(directory)
	
	if game == "Minecraft":
		
		# ~~~~~~~~~~ Copy Template
		
		copy_tree(run_directory + "templates/" + game + "/", directory, update = 1)
		
		# ~~~~~~~~~ Config
		
		runConfigContent = ""
		for option in data:
			runConfigContent += option + '="'
			if data[option] != '':
				runConfigContent += data[option]
			runConfigContent += '"\n'
		runConfigContent += "doAutostart=" + str('autostart' in data) + "\n"
		
		runConfig = open(directory + "runConfig.cfg", 'w')
		runConfig.truncate()
		runConfig.write(runConfigContent)
		runConfig.close()
		
		# ~~~~~~~~~~ End
		
		return True
	elif game == "Factorio":
		return True

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
	models.deleteServerFromTable(request.form['data'], session['username'])
	shutil.rmtree(getConfig('SERVERS_DIRECTORY') + request.form['data'] + "/")
	return getCurrentURL(request.form)

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
			print("create")
			print(owner.Username)
			print(user2.Username)
			print(
			models.Moderator.create(
					Owner = owner,
					Server = models.getServer(serverName, ownerName),
					User = user2,
					Permissions = permGroup
				)
			)
			print("created")
		except Exception as e:
			setError(str(e))
	
	return redirect(url_for('server', username=ownerName, serverName=serverName))

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
	
	consoleFile = getConfig('SERVERS_DIRECTORY') + serverName + "/console.txt"
	consoleText = None
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

init()

if __name__ == '__main__':
	ctx = app.test_request_context()
	ctx.push()
	app.preprocess_request()
	port = int(os.getenv('PORT', 8080))
	host = os.getenv('IP', '0.0.0.0')
	app.config['SERVER_START'] = int(round(time.time() * 1000))
	app.run(port=port, host=host)
	
	models.db.connect()
	models.db.close()
	
