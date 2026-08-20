import os

directory = 'app/src/main/java/com/example/presentation/screens'
for filename in os.listdir(directory):
    if filename.endswith('.kt'):
        filepath = os.path.join(directory, filename)
        with open(filepath, 'r') as file:
            filedata = file.read()
        
        newdata = filedata.replace('Icons.AutoMirrored.Filled.', 'Icons.Default.')
        
        with open(filepath, 'w') as file:
            file.write(newdata)
        print(f"Updated {filename}")
