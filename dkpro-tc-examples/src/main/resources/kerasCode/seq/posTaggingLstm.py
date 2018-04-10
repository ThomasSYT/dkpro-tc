from sys import argv
import numpy as np
import argparse

EMBEDDING_DIM=-1
np.set_printoptions(threshold=np.nan)

def numpyizeVector(vec):
	vout=[]
	file = open(vec, 'r')
	for l in file.readlines():
		l = l.strip()
		v = [int(x) for x in l.split()]
		vout.append(v)
		#vout.append(np.fromstring(l, dtype=int, sep=' '))
	file.close()
	return vout
	
def loadEmbeddings(emb):
	matrix = {}	
	f = open(emb, 'r')
	embData = f.readlines()
	f.close()
	dim = len(embData[0].split())-1
	matrix = np.zeros((len(embData)+1, dim))	
	for e in embData:
		e = e.strip()
		if not e:
			continue
		idx = e.find(" ")
		id = e[:idx]
		vector = e[idx+1:]
		matrix[int(id)]=np.asarray(vector.split(" "), dtype='float32')
	return matrix, dim

def runExperiment(seed, trainVec, trainOutcome, testVec, testOutcome, embedding, longest_sequence, predictionOut):	

	np.random.seed(seed)

	from keras.preprocessing import sequence
	from keras.models import Sequential
	from keras.layers import Dense, Activation, Embedding, TimeDistributed, Bidirectional, Convolution1D
	from keras.layers import LSTM
	from keras.utils import np_utils

	trainVecNump = numpyizeVector(trainVec)
	trainOutcome = numpyizeVector(trainOutcome)
	
	testVecNump = numpyizeVector(testVec)
	testOutcome = numpyizeVector(testOutcome)
	
	if embedding:
		print("Load pretrained embeddings")
		embeddings,dim = loadEmbeddings(embedding)
		EMBEDDING_DIM = dim
	else:
		print("Train embeddings on the fly")
		EMBEDDING_DIM = 50
	
	x_train = sequence.pad_sequences(trainVecNump, maxlen=longest_sequence)
	y_train = sequence.pad_sequences(trainOutcome, maxlen=longest_sequence)
	x_test = sequence.pad_sequences(testVecNump, maxlen=longest_sequence)
	
	y_test = testOutcome
	maxLabel = max(x for s in trainOutcome+testOutcome for x in s) + 1
		
	y_train = np.array([np_utils.to_categorical(s, maxLabel) for s in y_train])


	vocabSize = max(x for s in trainVecNump+testVecNump for x in s)

	print("Building model")
	model = Sequential()
	if embedding:
		model.add(Embedding(output_dim=embeddings.shape[1], input_dim=embeddings.shape[0], input_length=x_train.shape[1], weights=[embeddings], trainable=False))
	else:
		model.add(Embedding(vocabSize+1, EMBEDDING_DIM))
	model.add(Convolution1D(128, 5, padding='same', activation='relu'))	
	model.add(Bidirectional(LSTM(EMBEDDING_DIM, return_sequences=True)))
	model.add(TimeDistributed(Dense(maxLabel)))
	model.add(Activation('softmax'))

	# try using different optimizers and different optimizer configs
	model.compile(loss='categorical_crossentropy',
              optimizer='rmsprop',
              metrics=['accuracy'])

	model.fit(x_train, y_train, epochs=1, shuffle=True)

	prediction = model.predict_classes(x_test)

	predictionFile = open(predictionOut, 'w')
	predictionFile.write("#Gold\tPrediction\n")
	for i in range(0, len(prediction)):
		predictionEntry = prediction[i]
		for j in range(0, len(y_test[i])):
			if y_test[i][j]==0:
				break #we reached the padded area - zero is reserved
			predictionFile.write(str(y_test[i][j]) +"\t" + str(predictionEntry[j]))
			if j+1 < len(y_test[i]):
				predictionFile.write("\n")
		predictionFile.write("\n")
	predictionFile.close()


if  __name__ =='__main__':
	parser = argparse.ArgumentParser(description="")
	parser.add_argument("--trainData", nargs=1, required=True)
	parser.add_argument("--trainOutcome", nargs=1, required=True)
	parser.add_argument("--testData", nargs=1, required=True)
	parser.add_argument("--testOutcome", nargs=1, required=True)    
	parser.add_argument("--embedding", nargs=1, required=False)    
	parser.add_argument("--maxLen", nargs=1, required=True)
	parser.add_argument("--predictionOut", nargs=1, required=True)
	parser.add_argument("--seed", nargs=1, required=False)    
    
    
	args = parser.parse_args()
    
	trainData = args.trainData[0]
	trainOutcome = args.trainOutcome[0]
	testData = args.testData[0]
	testOutcome = args.testOutcome[0]
	if not args.embedding:
		embedding=""
	else:
		embedding = args.embedding[0]
	maxLen = args.maxLen[0]
	predictionOut = args.predictionOut[0]
	if not args.seed:
		seed=897534793	#random seed
	else:
		seed = args.seed[0]
	
	runExperiment(int(seed), trainData, trainOutcome, testData, testOutcome, embedding, int(maxLen), predictionOut)

