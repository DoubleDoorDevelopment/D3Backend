
import Base

class Data(Base.Data):
	
	def __init__(self):
		Base.Data.__init__(self)
		self.setEdittableFiles({})
	
	def getPortOriginal(self):
		return 34197
	
	def getPortRange(self):
		return (34100, 34199)

class Cache(Base.Cache):
	
	def __init__(self, directory):
		Base.Cache.__init__(self, directory)
	
	def refreshCache(self, typeCache, force = False):
		pass
	
	def getVersions(self, typeVersion, data = {}):
		return []
	
	def getVersionURL(self, typeVersion, data = {}):
		return None

class Server(Base.Server):
	
	def __init__(self, cache, directory, nameOwner, nameServer):
		Server.__init__(self, cache, directory, nameOwner, nameServer)
	
	def backup(self):
		pass

class Thread(Base.Thread):
	
	def __init__(self, server):
		Base.Thread.__init__(self, server)
