import urllib.request
import urllib.parse
import re

url = "https://drive.google.com/file/d/1n9iDajhtrowciBbtGc8bYgPXWEklE5Ta/view?usp=sharing"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})

try:
    with urllib.request.urlopen(req) as response:
        content = response.read().decode('utf-8', errors='ignore')
        
    print("Page Title:")
    title_match = re.search(r'<title>(.*?)</title>', content)
    if title_match:
        print(title_match.group(1))
    else:
        print("No title found.")
        
    # Print some snippets of the page content
    print("\nPage body snippet:")
    print(content[:2000])
    
    # Save the page content to a file to examine
    with open("tmp/gdrive_page.html", "w", encoding="utf-8") as f:
        f.write(content)
    print("\nSaved page content to tmp/gdrive_page.html")
except Exception as e:
    print(f"Error fetching page: {e}")
