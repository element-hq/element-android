#!/usr/bin/env python

# 
# this is a very hacky timer test; writing in python to allow it
# to be a bit handwavey in places, and more likely to have some
# variance. Trying to be open to have variance :)
# 

import asyncio
from datetime import datetime

from nio import AsyncClient, MatrixRoom, RoomMessageText
from nio.responses import RoomCreateResponse, RoomCreateError

async def message_callback(room: MatrixRoom, event: RoomMessageText) -> None:
    print("Saw message")


async def main() -> None:
    client = AsyncClient("http://localhost:8008", "@tester:localhost")
    client.add_event_callback(message_callback, RoomMessageText)
    await client.login("password")
    response = await client.room_create()
    print(response)
    room_id = response.room_id
    before = datetime.now()
    for id in range(10000):
       await client.room_send(
           room_id=room_id,
           message_type="m.room.message",
           content={"msgtype": "m.text",
                    "body": f"test_ {id}"
           }
       )
       if (id % 100 == 0):
         print(f"now at {id}")
    after = datetime.now()
    print(after - before)
    

asyncio.get_event_loop().run_until_complete(main())
