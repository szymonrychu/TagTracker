package com.richert.tagtracker;

import org.opencv.android.OpenCVLoader;

import com.richert.tagtracker.R;
import com.richert.tagtracker.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class OtherCodesActivity extends Activity {
	private final static String ArduinoCode="\nint pwm[] = {3, 9, 5, 6, 10, 11};\nint val[] = {177, 177, 0, 0, 0, 0};\nint pwmNum = 6;\nint c=0;\n\nvoid setup(){\n  for(c=0;c<pwmNum;c++){\n    pinMode(pwm[c],OUTPUT);\n    analogWrite(pwm[c], val[c]);\n  }\n  Serial.begin(9600);\n  Serial.flush();\n}\nint parse(char table[], int start){\n  int result = 0;\n  for(int c=start;c>0;c--){\n    int num = ((int)table[c-1])-48;\n    int power = pow(10,start-c);\n    result = result + num*power;\n  }\n  return result;\n}\nvoid serialEvent(){\n  char table[4];\n  for(c=0;c<pwmNum;c++){\n    int start = Serial.readBytesUntil(',',table,4);\n    val[c] = parse(table,start);\n  }\n  \n}\n\nvoid loop(){\n  for(int d=0;d<pwmNum;d++){\n    analogWrite(pwm[d], val[d]);\n  }\n  delay(10);\n}";
	private final static String PythonCode="import Image, ImageDraw, string\nfrom itertools import cycle\nimport sys\n\ndef draw_chessboard(tag_size=5, pixel_width=200):\n    \"Draw an n x n chessboard using PIL.\"\n    def sq_start(i):\n        \"Return the x/y start coord of the square at column/row i.\"\n        return i * pixel_width / tag_size\n    \n    def square(i, j):\n        \"Return the square corners, suitable for use in PIL drawings\" \n        return map(sq_start, [i, j, i + 1, j + 1])\n    binary = lambda cc: cc>0 and [cc&1]+binary(cc>>1) or []\n    n = (tag_size-2)*(tag_size-2)-4\n    n=2**n\n    for i in range(0,n):\n        print i\n        bin = list((\"{0:0%db}\"%n).format(i))\n        revbin =  list(reversed(bin))\n        pix = [[1 for x in range(0,tag_size)] for x in range(0,tag_size)]\n        \n        \n        for xx in range(1,tag_size-1):\n            for yy in range(1,tag_size-1):\n                pix[xx][yy]=2\n                if (xx==(tag_size-1)-yy and (xx==1 or yy==1)) or (xx==tag_size-2 and yy==tag_size-2):\n                    pix[xx][yy]=1\n                if xx==1 and yy==1:\n                    pix[xx][yy]=0\n        cc=0\n        \n        for xx in range(1,tag_size-1):\n            for yy in range(1,tag_size-1):\n                if(pix[xx][yy]==2):\n                    if int(revbin[cc]) > 0:\n                        pix[xx][yy]=1\n                    else :\n                        pix[xx][yy]=0\n                    cc=cc+1\n               \n    \n        squares=[]\n        for xx in range(0,tag_size):\n            strng = ''\n            for yy in range(0,tag_size):\n                if(pix[xx][yy]==0):\n                    squares.append(square(xx,yy))\n                strng+=str(pix[xx][yy])\n            print strng\n                \n        \n        image = Image.new(\"L\", (pixel_width, pixel_width))\n        draw_square = ImageDraw.Draw(image).rectangle\n        for sq in squares:\n            draw_square(sq, fill='white')\n    \n    \n    \n                \n        image.save(\"tag%d.%dx%d.png\"%(i,tag_size,tag_size))\n    \n    \ndef drawTags(size=7, width=300):\n    if size<7:\n        print('provided to small tag size! %d'%size)\n        return\n    if width%size !=0 :\n        print('provided invalid tag width! (widthXsize=%d)'%(width%size))\n        return\n    n = (size-4)*(size-4)-4\n    n=2**n\n    counter=0\n    for i in range(0,n):\n        print('drawing: %d/%d'%(i+1,n))\n        pix = [[0 for x in range(0,size)] for x in range(0,size)]\n        for xx in range(0,size):\n            for yy in range(0,size):\n                if xx>0 and xx<size-1 and yy>0 and yy<size-1:\n                    if xx==1 or yy==1 or xx==size-2 or yy==size-2 :\n                        pix[xx][yy]=1\n                    elif xx==2 and yy==size-3:\n                        pix[xx][yy]=1\n                    elif xx==size-3 and (yy==2 or yy==size-3):\n                        pix[xx][yy]=1\n                    elif xx==2 and yy==2:\n                        pix[xx][yy]=0\n                    else:\n                        pix[xx][yy]=3\n        tmp=counter\n        for yy in reversed(range(2,size-2)):\n            for xx in reversed(range(2,size-2)):\n                if(pix[xx][yy] == 3):\n                    if tmp%2==1:\n                        pix[xx][yy]=1\n                    else:\n                        pix[xx][yy]=0\n                    tmp=tmp/2\n                        \n        counter+=1\n        for xx in range(0,size):\n            for yy in range(0,size):\n                sys.stdout.write('%d,'%pix[xx][yy])\n            sys.stdout.write('\n')\n        \n        image = Image.new(\"L\", (width , width))\n        canvas = ImageDraw.Draw(image)\n        canvas.rectangle([0,0,width-1,width-1], 'white', 'black')\n        for yy in range(0,size):\n            for xx in range(0,size):\n                X=xx*(width/size)\n                Y=yy*(width/size)\n                if pix[xx][yy]==1:\n                    canvas.rectangle([X,Y,X+width/size-1,Y+width/size-1], 'black', 'black')\n                    \n        \n        image.save(\"tag%d.%dx%d.png\"%(i+1,size,size))\n    \n#draw_chessboard(tag_size=5, pixel_width=300)\ndrawTags(7,350)\n";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_othercode);
		TextView textView = (TextView) findViewById(R.id.othercode_text_view);
		textView.setText("ARDUINO:\n\n"+ArduinoCode+"\n\nPYTHON:\n\n"+PythonCode);
		textView.setMovementMethod(new ScrollingMovementMethod());
	}

}
