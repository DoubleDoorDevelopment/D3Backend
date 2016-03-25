#!/usr/bin/env python

from threading import Thread
from Queue import Queue, Empty
import sys

class ThreadMaster(Thread):
	
	def __init__(self, pool):
		Thread.__init__(self)
		self.daemon = True
		self.running = True
		
		#sys.stdout.write("Start threadmaster\n")
		self.pool = pool
	
	def run(self):
		while self.running:
			pass
	
	def downloadFile(self, serverowner, serverName, url, file):
		#sys.stdout.write("Downloading " + file + "\n")
		import urllib
		urllib.urlretrieve(url, file)
		#sys.stdout.write("Done downloading " + file + "\n")
	
	def addDownload(self, serverowner, servername, url, file):
		self.pool.addTask(self.downloadFile,
			serverowner, servername,
			url, file)

class ThreadPool():
	
	def __init__(self, max_threads):
		self.tasks = Queue()
		self.max_threads = max_threads
		self.running_threads = 0
	
	def addTask(self, func, *args, **kargs):
		self.tasks.put((func, args, kargs))
		if self.running_threads < self.max_threads:
			ThreadDownload(self)
	
	def setWorkerStart(self):
		self.running_threads += 1
	
	def setWorkerFinish(self):
		self.running_threads -= 1

class ThreadDownload(Thread):
	
	def __init__(self, pool):
		Thread.__init__(self)
		self.daemon = True
		self.running = True
		
		self.pool = pool
		self.pool.setWorkerStart()
		
		self.start()
	
	def run(self):
		while self.running:
			try:
				func, args, kargs = self.pool.tasks.get()
			except Empty as e:
				self.running = False
				pass
			try:
				#sys.stdout.write("start func\n")
				func(*args, **kargs)
				#sys.stdout.write("end func\n")
			except Exception as e:
				sys.stdout.write(str(e) + "\n")
			finally:
				#sys.stdout.write("done task\n")
				self.pool.tasks.task_done()
		self.pool.setWorkerFinish()
