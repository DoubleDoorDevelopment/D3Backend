
from configparser import ConfigParser, SafeConfigParser
import StringIO
import os

class FakeSecHead(object):
	def __init__(self, fp):
		self.fp = fp
		self.sechead = '[asection]\n'
	
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
	newContent = StringIO.StringIO()
	newContent.write('[general]\n')
	newContent.write(open(filePath).read())
	newContent.seek(0, os.SEEK_SET)
	
	config = ConfigParser()
	config.readfp(newContent)
	
	config['general'][label] = value
	with open(filePath, 'w') as file:
		config.write(file)
