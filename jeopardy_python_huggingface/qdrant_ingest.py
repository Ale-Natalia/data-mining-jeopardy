from pathlib import Path
from qdrant_client import QdrantClient
from qdrant_client.http.models import (
    VectorParams,
    Distance,
    PointStruct
)
from angle_emb import AnglE, Prompts
import re
import logging
from tqdm import tqdm

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def split_string_into_groups(input_string, group_size):
    words = input_string.split()
    return [words[i:i+group_size] for i in range(0, len(words), group_size)]

def create_db():
    client.create_collection(
        collection_name=collection_name,
        vectors_config=VectorParams(size=1024, distance=Distance.COSINE),
    )

def ingest():
    ID = 1
    need_to_check = True
    for file_path in Path(dir_path).glob("*.txt"):
        logger.info(f"Ingesting file {file_path}!")
        with open(file_path, "r") as file:
            file_content = file.read()
        
        pattern = r'\[\[([^\]]+)\]\]'
        matches = re.findall(pattern, file_content)
        word_dict = {}
        for match in matches:
            start_index = re.search(fr'\[\[{re.escape(match)}\]\]', file_content).end()
            end_index = file_content.find('[', start_index)
            if end_index == -1:
                word_dict[match] = file_content[start_index:]
            else:
                word_dict[match] = file_content[start_index:end_index]
        for key in tqdm(word_dict):
            sequences = split_string_into_groups(word_dict[key], 512)
            for sequence in sequences:
                if need_to_check:
                    res = client.retrieve(
                        collection_name=collection_name,
                        ids = [ID]
                    )
                    if len(res) == 0:
                        logger.info(f"Resuming ingest from ID: {ID}")
                        embedding = angle.encode({'text': sequence}, to_numpy=True)
                        client.upsert(
                            collection_name=collection_name,
                            wait=True,
                            points=[
                                PointStruct(
                                    id=ID,
                                    vector=embedding[0],
                                    payload={"title": key}),
                            ],
                        )
                        need_to_check = False
                else:
                    embedding = angle.encode({'text': sequence}, to_numpy=True)
                    client.upsert(
                        collection_name=collection_name,
                        wait=True,
                        points=[
                            PointStruct(
                                id=ID,
                                vector=embedding[0],
                                payload={"title": key}),
                        ],
                    )
                ID += 1

if __name__=="__main__":
    logger.info(f"Loading model!")
    angle = AnglE.from_pretrained('WhereIsAI/UAE-Large-V1', pooling_strategy='cls').cuda()
    angle.set_prompt(prompt=Prompts.C)
    dir_path = "./wikipages/dataset"
    collection_name = "test_collection"
    client = QdrantClient("localhost", port=6333)
    if len(client.get_collections().collections) == 0:
        logger.info(f"Create database: {collection_name}")
        create_db()
    else:
        logger.info(f"Use database: {collection_name}")
    logger.info("Start ingest!")
    ingest()