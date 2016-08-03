
from backend import *
from peewee import *
from playhouse.shortcuts import RetryOperationalError
from python.safety import *

class MysqlRetryDatabase(RetryOperationalError, MySQLDatabase):
	pass

db = MysqlRetryDatabase(getConfig('SQL_DATABASE'), user=getConfig('SQL_USERNAME'), password=getConfig('SQL_PASSWORD'))

class BaseModel(Model):
	class Meta:
		database = db

class User(BaseModel):

	__searchable__ = [
		'Username',
		'SteamUser'
	]

	Username = CharField(primary_key=True)
	Password = CharField()
	Salt = CharField()
	SteamUser = CharField()
	SteamPass = CharField()
	SteamSalt = CharField()
	Group = CharField()
	
	def setPassword(self, new):
		self.Salt = randomString(50)
		self.Password = encrypt(self.Salt, new)
		self.save()
	
	def getPassword(self):
		return decrypt(self.Salt, self.Password)
	
	def setSteamCredentials(self, username, password):
		self.SteamSalt = randomString(50)
		self.SteamUser = username
		self.SteamPass = encrypt(self.SteamSalt, password)
		self.save()
	
	def getSteamCredentials(self):
		return (self.SteamUser, decrypt(self.SteamSalt, self.SteamPass))

class Server(BaseModel):
	
	__searchable__ = [
		'Game', 'Port', 'Purpose'
	]
	
	Name = CharField(primary_key=True)
	User = ForeignKeyField(User, db_column='User', default=None, null=True)
	Port = CharField()
	Purpose = CharField()

class Moderator(BaseModel):
	
	__searchable__ = [
			'Owner', 'Server', 'User', 'Permissions'
		]
	
	Identifier = PrimaryKeyField()
	Owner = ForeignKeyField(User, db_column='Owner', related_name='owner')
	Server = ForeignKeyField(Server, db_column='Server')
	User = ForeignKeyField(User, db_column='User', related_name='user')
	Permissions = CharField()

def getUsers():
	return User.select().order_by(User.Username)

def getUser(username):
	error = None
	try:
		query = getUsers().where(User.Username == username)
		result = query
		if len(query) > 0:
			result = query.get()
	except Exception as e:
		error = str(e)
	return (result, error)

def getUserGroups(users = getUsers()):
	array = []
	
	if len(users) <= 0:
		return array
	
	data = users.order_by(User.Group)
	for entry in data:
		if str(entry.Group) in array:
			pass
		else:
			array.append(str(entry.Group))
	
	return array

def validatePassword(username, password):
	user, error = getUser(username)
	if user == None or user.getPassword() != password:
		return (False, "Invalid Credentials")
	return (True, None)

def regenHash(username):
	user, error = getUser(username)
	if user != None:
		user.setPassword(user.getPassword())

def deleteUserFromTable(username):
	user, error = getUser(username)
	if user != None:
		user.delete_instance()
	return error

def getServers():
	return Server.select().order_by(Server.Name)

def getServersForUser(username):
	return getServers().where(Server.User == username).order_by(Server.Name)

def getServersForModerator(username):
	servers = []
	for entry in Moderator.select().where(Moderator.User == username):
		server = getServer(entry.Owner, entry.Server)
		if not server in servers:
			servers.append(server)
	return servers

def getServer(nameOwner, nameServer):
	error = None
	result = None
	try:
		query = getServersForUser(nameOwner).where(Server.Name == nameServer)
		result = query
		if len(query) > 0:
			result = query.get()
	except Exception as e:
		error = str(e)
	return (result, error)

def deleteServerFromTable(nameOwner, nameServer):
	server, error = getServer(nameOwner, nameServer)
	if server != None:
		server.delete_instance()

def getModerators(name, username, permissions = None):
	error = None
	result = None
	try:
		query = Moderator.select().where(
				Moderator.Server == name and Moderator.User == username and
				(Moderator.Permissions == permissions if permissions != None else Moderator.Permissions != '')
			)
		result = query
	except Exception as e:
		error = str(e)
	return (result, error)
