
from configparser import ConfigParser, SafeConfigParser, RawConfigParser
import StringIO
import os

class FakeSecHead(object):
	def __init__(self, fp):
		self.fp = fp
		self.sechead = '[general]\n'
	
	def readline(self):
		if self.sechead:
			try: 
				return self.sechead
			finally: 
				self.sechead = None
		else: 
			return self.fp.readline()

def setProperty_ini(filePath, section, label, value):
	config = ConfigParser()
	
	config.read(filePath)
	
	config[section][label] = value
	with open(filePath, 'w') as file:
		config.write(file)

def setProperty_properties(filePath, label, value):
	propStr = '[general]\n' + open(filePath).read()
	newContent = StringIO.StringIO(propStr)
	
	config = RawConfigParser()
	config.readfp(newContent)
	
	config['general'][label] = value
	with open(filePath, 'w') as file:
		config.write(file)

def getProperty_properties(filePath, label):
	propStr = '[general]\n' + open(filePath).read()
	newContent = StringIO.StringIO(propStr)
	config = RawConfigParser()
	config.readfp(newContent)
	return config['general'][label]