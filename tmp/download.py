import urllib.request
import re
import sys
import os

def download_file_from_google_drive(id, destination):
    URL = "https://docs.google.com/uc?export=download&confirm=t"
    
    # We first try to get the download page to retrieve any confirmation token
    req = urllib.request.Request(
        f"https://drive.google.com/uc?export=download&id={id}",
        headers={'User-Agent': 'Mozilla/5.0'}
    )
    
    try:
        with urllib.request.urlopen(req) as response:
            content = response.read().decode('utf-8', errors='ignore')
            
        # Try to find a confirmation token
        match = re.search(r'confirm=([0-9a-zA-Z_]+)', content)
        if match:
            confirm_token = match.group(1)
            download_url = f"https://drive.google.com/uc?export=download&confirm={confirm_token}&id={id}"
        else:
            # Let's try matching a different confirm parameter form if any
            download_url = f"https://drive.google.com/uc?export=download&id={id}"
            
        print(f"Downloading from {download_url} to {destination}...")
        
        # Download the file
        req2 = urllib.request.Request(download_url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req2) as response, open(destination, 'wb') as out_file:
            # Print content type and disposition to know what we got
            headers = response.info()
            print("Content-Type:", headers.get('Content-Type'))
            print("Content-Disposition:", headers.get('Content-Disposition'))
            
            chunk_size = 1024 * 1024
            while True:
                chunk = response.read(chunk_size)
                if not chunk:
                    break
                out_file.write(chunk)
                sys.stdout.write('.')
                sys.stdout.flush()
        print("\nDownload complete!")
    except Exception as e:
        print(f"Error downloading: {e}")

if __name__ == "__main__":
    file_id = "1kAHxNlanolPCP_uqbKJKrmW1qUaobv2k"
    dest = "c:\\Users\\dell\\AndroidStudio\\jDroidStudio\\jKidTracker\\jFindMe\\tmp\\downloaded_file"
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    download_file_from_google_drive(file_id, dest)
