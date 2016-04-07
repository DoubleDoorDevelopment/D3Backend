
from enum import Enum, unique

@unique
class TypeVersion(Enum):
	release,\
	stable,\
	unstable,\
	beta,\
	alpha\
	= range(5)

@unique
class TypeCache(Enum):
	ALL,\
	version\
	= range(2)

import Minecraft
import Factorio

serverTypes = {}
serverTypeToClass = {}

@unique
class TypeServer(Enum):
	minecraft	= ("Minecraft", Minecraft)
	factorio	= ("Factorio", Factorio)
	
	def __init__(self, name, module):
		self.nameID = name
		self.module = module
		
		serverTypes[name] = self
		serverTypeToClass[name] = module.Server
	
	def getDataClass(self):
		return self.module.Data
	
	def getCacheClass(self):
		return self.module.Cache
	
	def getServerClass(self):
		return self.module.Server
	
	def getClassThread(self):
		return self.module.Thread
	
	def getName(self):
		return self.nameID
