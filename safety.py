
# HEAVILY based on https://gist.github.com/nekoya/5088592#file-decrypt-py

import random, string

def randomString(length):
	return ''.join(random.SystemRandom().choice(string.ascii_uppercase + string.digits + string.ascii_lowercase) for _ in range(length))

import base64
from Crypto.Cipher import Blowfish
from binascii import unhexlify
from struct import pack

def encrypt(key, string):
	size = Blowfish.block_size
	
	plen = size - len(string) % size
	padding = [plen] * plen
	pad_str = string + pack('b' * plen, *padding)
	
	cipher = Blowfish.new(key, Blowfish.MODE_ECB)
	return base64.urlsafe_b64encode(cipher.encrypt(pad_str))

def decrypt(key, string):
	plen = 4 - len(string) & 3
	pad_str = string + '=' * plen
	
	cipher = Blowfish.new(key, Blowfish.MODE_ECB)
	safe = base64.urlsafe_b64decode(pad_str.encode("utf-8"))
	decrypted = cipher.decrypt(safe)
	
	quanity_padding = ord(decrypted[-1])
	return decrypted[:(-1 * quanity_padding)]
