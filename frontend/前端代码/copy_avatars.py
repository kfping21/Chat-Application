import os
import shutil

source_dir = r"C:\Users\panjiawei\Desktop\头像"
dest_dir = r"C:\Users\panjiawei\Desktop\treehole\app\src\main\res\drawable"

valid_exts = {".jpg", ".jpeg", ".png"}
files = [f for f in os.listdir(source_dir) if os.path.splitext(f)[1].lower() in valid_exts]

count = 1
for f in files[:20]: # Copy 20 avatars
    ext = os.path.splitext(f)[1].lower()
    src_path = os.path.join(source_dir, f)
    dst_path = os.path.join(dest_dir, f"avatar_{count}{ext}")
    shutil.copy2(src_path, dst_path)
    count += 1

print(f"Copied {count-1} avatars.")
