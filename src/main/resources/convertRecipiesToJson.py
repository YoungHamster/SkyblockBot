filetext = str()

with open("SBBotTestInfo.txt") as f:
    filetext = f.read()

convertedJSON = str()

for symbol in filetext:
    if symbol == ':':
        convertedJSON += '"' + symbol
    elif symbol == '{' or symbol == ',':
        convertedJSON += symbol + '"'
    else:
        convertedJSON += symbol

with open("Supposedly_converted_to_JSON.txt", 'w+') as f:
    f.write(convertedJSON)