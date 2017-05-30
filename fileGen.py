name = input("Filename>> ")
infile = open(name, "w")

size = int(input("File size in bytes>> "))

for i in range(0, size):
	infile.write("a")

infile.close()
