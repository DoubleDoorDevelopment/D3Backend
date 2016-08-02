
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
	
	def install(self, **kwargs):
		# Replaces the directory in the install script
		self.replaceFileSubstring("install.steam", "%DIRECTORY%", self.dirRun)
		os.system(Config.getSteamShell() + " +runscript " + os.path.join(self.dirRun, "install.steam"))
	
	def replaceFileSubstring(self, filePath, option, string):
		with open(filePath, "wt") as fout:
		    with open(filePath, "rt") as fin:
		        for line in fin:
		            fout.write(line.replace(option, string))

class Thread(Base.Thread):
	
	def __init__(self, server):
		Base.Thread.__init__(self, server)
	
	def getRunDir(self):
		return os.path.join(Base.Thread.getRunDir(self), "linux")
	
	def setRunArgs(self):
		self.runArgs = ['./starbound_server'] 
