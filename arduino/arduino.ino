int pwm[] = {3, 9, 5, 6, 10, 11};
int val[] = {177, 190, 0, 0, 0, 0};
int prev[] = {177, 190, 0, 0, 0, 0};
int delta[] = {177, 190, 0, 0, 0, 0};
int def[] = {177, 190, 0, 0, 0, 0};
int weight[] = {20, 5, 5, 5, 5, 5};
int hist[] = {0, 5, 0, 0, 0, 0};

int pwmNum = 6;
int c=0;
void setup(){
  for(c=0;c<pwmNum;c++){
    pinMode(pwm[c],OUTPUT);
    analogWrite(pwm[c], val[c]);
  }
  Serial.begin(9600);
  Serial.flush();
}
int parse(char table[], int start){
  int result = 0;
  for(int c=start;c>0;c--){
    int num = ((int)table[c-1])-48;
    int power = pow(10,start-c);
    result = result + num*power;
  }
  return result;
}
void serialEvent(){
  char table[20];
  for(c=0;c<pwmNum;c++){
    int start = Serial.readBytesUntil(',',table,19);
    val[c] = parse(table,start);
  }
}
int prevSteer = 177;
int prevPivot = 177;
void loop(){
  for(int d=0;d<pwmNum;d++){
    if(val[d] == 0){
      analogWrite(pwm[d], def[d]);
      prev[d] = def[d];
    }else{
      delta[d] = val[d] - prev[d];
      if(abs(delta[d]/weight[d])>hist[d]){
        val[d] = delta[d]/weight[d] + prev[d];
        analogWrite(pwm[d], val[d]);
      }
      prev[d] = val[d];
    }
  }
  delay(50);
}
