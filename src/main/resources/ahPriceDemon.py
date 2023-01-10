import urllib.request
import json
from nbt import nbt
from base64 import b64decode
from io import BytesIO
import mysql.connector
import time

def requestAuctionsEnded():
    r = urllib.request.urlopen('https://api.hypixel.net/skyblock/auctions_ended')
    data = json.loads(r.read())
    if data["success"]:
        return data
    else:
        print("Failed to load recently ended auctions")
        return False

def unpack_nbt(tag):
    """
    Unpack an NBT tag into a native Python data structure.
    """

    if isinstance(tag, nbt.TAG_List):
        return [unpack_nbt(i) for i in tag.tags]
    elif isinstance(tag, nbt.TAG_Compound):
        return dict((i.name, unpack_nbt(i)) for i in tag.tags)
    else:
        return tag.value

def decodeItemBytes(RawBytes):
    data = nbt.NBTFile(fileobj = BytesIO(b64decode(RawBytes)))
    unpacked = unpack_nbt(data)
    return unpacked
    
def sendAuctionToDB(timestamp, price, bin, productId, item_bytes, db_cursor):
    try:
        db_cursor.execute("INSERT INTO all_auctions (timestamp,price,bin,productId,item_bytes) VALUES "
        "({0},{1},{2},'{3}','{4}')".format(timestamp, price, int(bin), productId, item_bytes))
    except (mysql.connector.errors.Error, TypeError) as exc:
            print("Failed inserting {0}\nError: {1}\n".format(productId,exc))


db = mysql.connector.Connect(user='root', password='8585623',
                              host='127.0.0.1',
                              database='sb_ended_auctions')
db.autocommit = True
cursor = db.cursor()

while True:
    ended = requestAuctionsEnded()
    if not ended:
        print("Something went wrong")
        exit()

    for item in ended['auctions']:
        productId = decodeItemBytes(item['item_bytes'])['i'][0]['tag']['ExtraAttributes']['id']
        sendAuctionToDB(item['timestamp'], item['price'], item['bin'],
                        productId, item['item_bytes'], cursor)
    time.sleep(60)