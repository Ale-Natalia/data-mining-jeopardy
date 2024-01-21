from qdrant_client import QdrantClient
from angle_emb import AnglE, Prompts
import logging
import numpy as np
import json
from tqdm import tqdm

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def run_inference():
    res_dict = {"answers": []}
    questions = []
    categories = []
    answers = []

    logger.info(f"Start parsing file: {questions_file}!")
    with open(questions_file, 'r') as file:
        lines = file.readlines()
    for i, line in enumerate(lines, 1):
        if i % 4 == 0:
            continue
        if i % 4 == 1:
            categories.append(line.strip())
        if i % 4 == 2:
            questions.append(line.strip())
        if i % 4 == 3:
            answers.append(line.strip())
    logger.info(f"Finish parsing file: {questions_file}!")

    for (c, q, a) in tqdm(zip(categories, questions, answers), total = len(categories)):
        embedding = angle.encode({'text': f"{c}. {q}."}, to_numpy=True)
        res = client.search(
            collection_name=collection_name,
            query_vector=embedding[0],
            limit=15,
        )
        answers = [answer.payload["title"] for answer in res]
        scores = [answer.score for answer in res]
        probs = (np.exp(scores) / np.sum(np.exp(scores), axis=0)).tolist()
        res_dict["answers"].append({
            "category": c,
            "question": q,
            "answer": a,
            "pred": {
                "answers": answers,
                "probs": probs
            }
        })

        with open("res.json", "w") as file:
            file.write(json.dumps(res_dict, indent=4))
    

if __name__=="__main__":
    logger.info(f"Loading model!")
    angle = AnglE.from_pretrained('WhereIsAI/UAE-Large-V1', pooling_strategy='cls').cuda()
    angle.set_prompt(prompt=Prompts.C)
    collection_name = "test_collection"
    client = QdrantClient("localhost", port=6333)
    questions_file = "./wikipages/questions/questions.txt"
    run_inference()