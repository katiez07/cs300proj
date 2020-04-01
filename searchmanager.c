#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/msg.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <signal.h>
#include "longest_word_search.h"
#include "queue_ids.h"

// @author Katie Zucker, krzucker@crimson.ua.edu, CWID 11624565

/* Search Manager
* Reqs:
* - Must be written in C
* - Numeric parameter denoting delay will be present and an integer; if it is zero, then use no delay
* - At least one prefix will be provided (may not be valid)
* - Only process prefixes are at least 3 characters should be processed
* - Only one prefix should be processed at a time. Once all the results on a prefix are returned, the next can be
* sent to the passage processor
* - Send a prefix message with a zero id to notify the passage processor to complete
*/

void sigintHandlerNoInfo(int);
void sigintHandlerWithInfo(int);
void send(char*, int);
void rcv();

#ifndef mac
size_t
strlcpy(char *dst,
	const char *src,
	size_t size)
{
	size_t srclen;
size --;
srclen = strlen(src);

if (srclen > size)
	srclen = size;

memcpy (dst, src, srclen);
dst[srclen] = '\0';

return (srclen);
}
#endif


// global vars so signal handlers can access them
int numPrefixes;
int i;

// sigint handler before starting
void sigintHandlerNoInfo(int sig_num) 
{ 
    /* Reset handler to catch SIGINT next time. 
       Refer http://en.cppreference.com/w/c/program/signal */
	if (sig_num == SIGINT){
		printf("No information yet\n");
    		fflush(stdout); 
	}
} 

void sigintHandlerWithInfo(int sig_num){
	if (sig_num == SIGINT){
		printf("%d of %d processed\n", i-1, numPrefixes);
    		fflush(stdout); 
	}
}



void main(int argc, char *argv[]){

	int delay = atoi(argv[1]);
	numPrefixes = argc - 2;

	#ifdef DEBUG
	printf("delay:%d\nnumPrefixes:%d\n", delay, numPrefixes);
	#endif

	// i keeps track of prefixes numbers, and it is also used as the
	// loop index for looping through the prefixes in argv
	i = 1;

	signal(SIGINT, sigintHandlerNoInfo);

	while (i <= numPrefixes){
		if (i > 1) signal(SIGINT, sigintHandlerWithInfo);
		char *prefix = argv[i+1];
		if (strlen(prefix) < 3 || strlen(prefix) > 20){
			fprintf(stderr, "Please provide prefixes of length between3 and 20\n");
			exit(0);
		}
	
		//send msg
		#ifdef DEBUG
		printf("sending\n");
		#endif
		send(prefix, i);

		//wait to recieve msgs back from numPrefixes different threads
		int j;
		for (j=1;j<numPrefixes;j++){
			rcv();
		}

		// sleeps for the amount of time specified
		sleep(delay);

		// then continues
		i++;
	}
	
	// send msgs 1 at a time using msgsnd
	// use delay int if !0
	
	// loop to wait for msgrcv

}

void send(char *pre, int id){
	int msqid;
	int msgflg = IPC_CREAT | 0666;
	key_t key;
	prefix_buf sbuf;
	size_t buf_length;

	key = ftok(CRIMSON_ID, QUEUE_NUMBER);
	if ((msqid = msgget(key, msgflg)) < 0){
		int errnum = errno;
		fprintf(stderr, "Value of errno: %d\n", errno);
		perror("(msgget)");
		fprintf(stderr, "Error msgget: %s\n", strerror(errnum));
	}
	else{
		
		fprintf(stderr, "msgget: msgget succeeded: msgqid = %d\n", msqid);
	}


	// message type 1
	sbuf.mtype = 1;
	strlcpy(sbuf.prefix, pre, WORD_LENGTH);
	sbuf.id = id;
	buf_length = strlen(sbuf.prefix) + sizeof(int) + 1; //includes null at end

	// send a message
	if ((msgsnd(msqid, &sbuf, buf_length, IPC_NOWAIT)) < 0){
		int errnum = errno;
		fprintf(stderr, "Value of errno: %d\n", errno);
		perror("(msgget)");
		fprintf(stderr, "Error msgget: %s\n", strerror(errnum));
		exit(1);
	}
	else{
		fprintf(stderr, "Message(%d): \"%s\" Sent  (%d bytes)\n", sbuf.id, sbuf.prefix, (int)buf_length);
	}

	return;
}


void rcv(){
	int msqid;
	int msgflg = IPC_CREAT | 0666;
	key_t key;
	response_buf rbuf;
	size_t buf_length;

	key = ftok(CRIMSON_ID, QUEUE_NUMBER);
	if ((msqid = msgget(key, msgflg)) < 0){
		int errnum = errno;
		fprintf(stderr, "Value of errno: %d\n", errno);
		perror("(msgget)");
		fprintf(stderr, "Error msgget: %s\n", strerror(errnum));
	}
	else
		fprintf(stderr, "msgget: msgget succeeded: msgqid = %d\n", msqid);

	// msgrcv for receiving msg
	int ret;
	do {
		ret = msgrcv(msqid, &rbuf, sizeof(response_buf), 2, 0);//receive type 2 message
		int errnum = errno;
		if (ret < 0 && errno !=EINTR){
			fprintf(stderr, "Value of errno: %d\n", errno);
			perror("Error printed by perror");
			fprintf(stderr, "Error receiving msg: %s\n", strerror( errnum ));
		}
	} while ((ret < 0 ) && (errno == 4));
	//fprintf(stderr,"msgrcv error return code --%d:$d--",ret,errno);

	if (rbuf.present == 1)
		fprintf(stderr,"%ld, %d of %d, %s, size=%d\n", rbuf.mtype, rbuf.index,rbuf.count,rbuf.longest_word, ret);
	else
		fprintf(stderr,"%ld, %d of %d, not found, size=%d\n", rbuf.mtype, rbuf.index,rbuf.count, ret);

	exit(0);
}



