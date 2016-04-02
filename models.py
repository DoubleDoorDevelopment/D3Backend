
from backend import *
from peewee import *

db = MySQLDatabase(getConfig('SQL_DATABASE'), user=getConfig('SQL_USERNAME'), password=getConfig('SQL_PASSWORD'))

class BaseModel(Model):
	class Meta:
		database = db

class User(BaseModel):

	__searchable__ = [
		'Username'
	]

	Username = CharField(primary_key=True)
	Password = CharField()
	Group = CharField()
	
	class Meta:
		database = db

class Server(BaseModel):
	
	__searchable__ = [
		'Game', 'Port', 'Purpose'
	]
	
	Name = CharField(primary_key=True)
	User = ForeignKeyField(User, db_column='User')
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
	if user == None or user.Password != password:
		return (False, "Invalid Credentials")
	return (True, None)

def deleteUserFromTable(username):
	user, error = getUser(username)
	if user != None:
		user.delete_instance()

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

def getServer(name, username):
	error = None
	result = None
	try:
		query = getServersForUser(username).where(Server.Name == name)
		result = query
		if len(query) > 0:
			result = query.get()
	except Exception as e:
		error = str(e)
	return (result, error)

def deleteServerFromTable(name, username):
	server, error = getServer(name, username)
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
