"""
This scripts generate artificial tuples based on rules blow:

# predicates
frame(buy): buy_0.6, purchase_0.4
frame(hire): hire_0.5, employ_0.4, engage_0.1
frame(attack): assault_0.2, attack_0.8
frame(feed): feed_0.9, keep_0.1

# semantic roles and realizations
Person: tom_0.5, tony_0.5
Food: banana_0.3, apple_0.3, pear_0.4
Animal: cat_0.4, dog_0.4, monkey_0.2
Organization: company_1.0
       
# frame structures
frame(buy): 
     subject(Person_0.4, Organization_0.6);
     object(Food_0.7, Animal_0.3)
frame(hire):
     subject(Organization_0.5, Person_0.5);
     object(Person_1.0)
frame(attack):
     subject(Person_0.2, Animal_0.8);
     object(Person_0.2, Animal_0.8)
frame(feed):
     subject(Person_0.4, Organization_0.6);
     object(Animal_0.5, Person_0.5)
"""

import random
import operator
import sys

sys.stdout = open('tuples.txt', 'w')

# semantic frames' types and accumulate percent
frames = {'buy':0.25, 'hire':0.5, 'attack':0.75, 'feed':1.0}
frames = sorted(frames.items(), key=operator.itemgetter(1))
# semantic roles' realizations and percent
roles = {
	'person' : sorted({'tom':0.5, 'tony':1}.items(), key=operator.itemgetter(1)),
	'food' : sorted({'banana':0.3, 'apple':0.6, 'pear':1}.items(), key=operator.itemgetter(1)),
	'animal' : sorted({'cat':0.4, 'dog':0.8, 'monkey':1}.items(), key=operator.itemgetter(1)),
	'org' : sorted({'company':1.0}.items(), key=operator.itemgetter(1))
}
# predicates' realization and accumulate percent
predicates = {
	'buy' : sorted({'buy':0.6, 'purchase':1}.items(), key=operator.itemgetter(1)),
	'hire' : sorted({'hire':0.5, 'employ':0.9, 'engage':1}.items(), key=operator.itemgetter(1)),
	'attack' : sorted({'assault':0.2, 'attack':1}.items(), key=operator.itemgetter(1)),
	'feed' : sorted({'feed':0.9, 'keep':1}.items(), key=operator.itemgetter(1))
}
# subjects' realization and accumulate percent
subjects = {
	'buy' : sorted({'person':0.4, 'org':1}.items(), key=operator.itemgetter(1)),
	'hire' : sorted({'org':0.5, 'person':1}.items(), key=operator.itemgetter(1)),
	'attack' : sorted({'person':0.2, 'animal':1}.items(), key=operator.itemgetter(1)),
	'feed' : sorted({'person':0.4, 'org':1}.items(), key=operator.itemgetter(1))
}
# objects' realization and accumulate percent
objects = {
	'buy' : sorted({'food':0.7, 'animal':1}.items(), key=operator.itemgetter(1)),
	'hire' : sorted({'person':1.0}.items(), key=operator.itemgetter(1)),
	'attack' : sorted({'person':0.2, 'animal':1}.items(), key=operator.itemgetter(1)),
	'feed' : sorted({'animal':0.5, 'person':1}.items(), key=operator.itemgetter(1))
}

def choose_frame():
	rand = random.random()
	for k, v in frames:
		if rand < v:
			return k

def choose_predicate(frame):
	# choose predicate type
	rand = random.random()
	for k, v in predicates[frame]:
		if rand < v:
			# choose predicate realization
			return k

def choose_subject(frame):
	#import pdb; pdb.set_trace()
	rand = random.random()
	candidates = subjects[frame]
	for k, v in candidates:
		if rand < v:
			rand = random.random()
			for m, n in roles[k]:
				if rand < n:
					return m

def choose_object(frame):
	rand = random.random()
	candidates = objects[frame]
	for k, v in candidates:
		if rand < v:
			rand = random.random()
			for m, n in roles[k]:
				if rand < n:
					return m

def main():
	tuple_num = 2500
	for i in range(tuple_num):
		frame = choose_frame()
		subj = choose_subject(frame)
		pred = choose_predicate(frame)
		obj = choose_object(frame)
		print ('%s\t%s\t%s') % (subj, pred, obj)

if __name__ == "__main__":
	main()