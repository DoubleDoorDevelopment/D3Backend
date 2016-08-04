
import Base
import os
import json
import urllib
import requests
import subprocess
from werkzeug import secure_filename
from socket import error as SocketError
import configger
import manageConfig as Config

class Data(Base.Data):
	
	def __init__(self):
		Base.Data.__init__(self)
		self.setEdittableFiles({})
	
	def getPortOriginal(self):
		return 21025
	
	def getPortRange(self):
		return 21000, 22000
	
	def getRunConfigKeys(self):
		return []

class Cache(Base.Cache):
	
	def __init__(self, directory):
		Base.Cache.__init__(self, directory)
		
	def getNames(self):
		return ['Starbound']
	
	def getVersions(self, typeVersion = None, data = {}):
		return ["latest"]

class Server(Base.Server):
	
	def __init__(self, cache, directory, nameOwner, nameServer):
		Base.Server.__init__(self, cache, directory, nameOwner, nameServer)
	
	def setPort(self, port):
		# TODO
		pass
	
	def backup(self):
		pass
	
	def cleanRunFiles(self):
		pass
	
	def getInstallCmd(self, args, kwargs):
		steamUser, steamPass = kwargs['user'].getSteamCredentials()
		#print(steamUser, steamPass)
		return [
			str(Config.getSteamShell()),
			"+login", str(steamUser), str(steamPass),
			"+force_install_dir", str(self.dirRun),
			"+app_update", "211820",  "validate",
			"exit"
		]
		#return str(Config.getSteamShell()) + " +login " + str(steamUser) + " '" + str(steamPass) + "' exit"
		
	
	def replaceFileSubstring(self, filePath, option, string):
		f = open(filePath,'r')
		filedata = f.read()
		f.close()
		newdata = filedata.replace(option, string)
		f = open(filePath,'w')
		f.write(newdata)
		f.close()

class Thread(Base.Thread):
	
	def __init__(self, server):
		Base.Thread.__init__(self, server)
	
	def getRunDir(self):
		return os.path.join(Base.Thread.getRunDir(self), "linux")
	
	def setRunArgs(self):
		self.runArgs = ['./starbound_server'] 
