from qdrant_client import QdrantClient
from angle_emb import AnglE, Prompts
import logging
import numpy as np

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def print_menu():
    print("1. Ask question")
    print("2. Exit")

def get_model_predictions(question):
    embedding = angle.encode({'text': question}, to_numpy=True)
    res = client.search(
        collection_name=collection_name,
        query_vector=embedding[0],
        limit=15,
    )
    return res

def print_answers(res):
    answers = [answer.payload["title"] for answer in res]
    scores = [answer.score for answer in res]
    probs = np.exp(scores) / np.sum(np.exp(scores), axis=0)
    for answer, prob in zip(answers, probs):
        print(f"Answer: {answer} with probability: {prob}")

if __name__=="__main__":
    logger.info(f"Loading model!")
    angle = AnglE.from_pretrained('WhereIsAI/UAE-Large-V1', pooling_strategy='cls').cuda()
    angle.set_prompt(prompt=Prompts.C)
    collection_name = "test_collection"
    client = QdrantClient("localhost", port=6333)
    while True:
        print_menu()
        option = input("Enter option: ")
        if option == "1":
            question = input("Enter question: ")
            res = get_model_predictions(question)
            print_answers(res)
        elif option == "2":
            print("Bye!")
            break
        else:
            print("Invalid option!")