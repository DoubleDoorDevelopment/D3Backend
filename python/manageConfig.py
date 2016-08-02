
from database import getConfig

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

def getSteamShell():
	return getConfig('STEAM_SHELL')