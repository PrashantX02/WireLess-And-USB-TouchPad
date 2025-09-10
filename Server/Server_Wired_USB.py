import socket
import pyautogui

pyautogui.PAUSE = 0
pyautogui.FAILSAFE = False

UDP_IP = "0.0.0.0"
UDP_PORT = 5000

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))

print(f"📡 Server listening on {UDP_PORT} ...")

while True:
    try:
        data, addr = sock.recvfrom(1024)
        message = data.decode().strip()
        # print("Received:", message)

        if message.startswith("MOVE"):
            _, dx, dy = message.split()
            pyautogui.moveRel(int(dx), int(dy))

        elif message == "CLICK_LEFT":
            pyautogui.click()

        elif message == "CLICK_RIGHT":
            pyautogui.click(button='right')

        elif message.startswith("SCROLL"):
            _, dy = message.split()
            pyautogui.scroll(int(dy))

        else:
            print("Unknown command:", message)

    except Exception as e:
        print("Error:", e)
