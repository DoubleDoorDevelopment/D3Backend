
import manageConfig
import types as Types

serverObjs = None
cacheObjs = None
serverData = None

def initData():
	global serverData
	serverData = {}
	
	for key in Types.serverTypes:
		serverType = Types.serverTypes[key]
		serverData[serverType.getName()] = serverType.getDataClass()()

def initCaches():
	cacheDirectory = manageConfig.getDirCache()
	
	global cacheObjs
	cacheObjs = {}
	
	for serverTypeKey in Types.serverTypes:
		serverType = Types.serverTypes[serverTypeKey]
		cacheObjs[serverType.getName()] = serverType.getCacheClass()(cacheDirectory + serverType.getName() + "/")
		cacheObjs[serverType.getName()].refresh()

def initServers(serverQuery):
	global serverObjs
	serverObjs = {}
	
	for serverModel in serverQuery:
		createServerObj(serverModel.User.Username, serverModel.Name, serverModel.Purpose, serverModel.Port)

def getServerData(serverTypeName = None):
	if serverTypeName == None:
		return serverData
	else:
		return serverData[serverTypeName]

def getServerCache():
	return cacheObjs

def getServerObjs():
	return serverObjs

def doesServerExist(nameOwner, nameServer):
	return nameOwner in serverObjs and nameServer in serverObjs[nameOwner]

def getServers(nameOwner):
	return serverObjs[nameOwner]

def getServer(nameOwner, nameServer):
	return serverObjs[nameOwner][nameServer]

def createServerObj(nameOwner, nameServer, serverTypeName, serverPort):
	global serverObjs
	
	if not nameOwner in serverObjs:
		serverObjs[nameOwner] = {}
	
	directory = manageConfig.getDirForServer(nameOwner, nameServer)
	
	if not serverTypeName in Types.serverTypeToClass:
		return False
	
	serverClass = Types.serverTypeToClass[serverTypeName]
	serverObj = serverClass(cacheObjs[serverTypeName], directory, nameOwner, nameServer)
	serverObj.setTypeServer(serverTypeName)
	serverObjs[nameOwner][nameServer] = serverObj

def removeServerObj(nameOwner, nameServer):
	global serverObjs
	if nameOwner in serverObjs and nameServer in serverObjs[nameOwner]:
		del serverObjs[nameOwner][nameServer]
