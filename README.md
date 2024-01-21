# Instructions for running Jeopardy #

1. For both the classical Lucene information retrieval project and the Deep Learning based HuggingFace project, you will need to clone this repository locally:
git clone https://github.com/Ale-Natalia/data-mining-jeopardy

2. Lucene information retrieval (Java):
  <br>2.1. Compile the pom.xml file using maven. You can do this using IntelliJ or the command:
   ````
   mvn package
   ````
  <br>2.2. Run the Main.java file from IntelliJ or another IDE of your choice.

3. Deep Learning information retrieval (Python):
  <br>3.1. Setup for the conda environment
  First create a virtual environment. E.g. using conda.
  ````
  conda create -n <env_name> python=3.10
  ````
  Activate the environment and install poetry via pip
  ````
  conda activate <env_name>
  pip install poetry
  ````
  Navigate to the directory where pyproject.toml is located and afterwards install all the dependencies using poetry.
  ````
  cd data-mining-jeopardy/jeopardy_python_huggingface/
  poetry install
  ````
  <br>3.2 Setup for Qdrant
  To setup qdrant you first need to have docker installed on your machine. After docker is installed run these commands
  ````
  docker pull qdrant/qdrant
  docker run -p 6333:6333 -p 6334:6334 \
    -v $(pwd)/qdrant_storage:/qdrant/storage:z \
    qdrant/qdrant
  ````
  For more informations regarding Qdrant visit the official website: https://qdrant.tech/documentation/quick-start/
  <br>3.3 Run the app
  After the qdrant service is up and ready to be used now start the app.
  ````
  python app.py
  ````
  <br>3.4 Now beat the Jeopardy game! :)

# Results

Java - 
Python - https://github.com/Ale-Natalia/data-mining-jeopardy/blob/main/jeopardy_python_huggingface/demo.mkv
