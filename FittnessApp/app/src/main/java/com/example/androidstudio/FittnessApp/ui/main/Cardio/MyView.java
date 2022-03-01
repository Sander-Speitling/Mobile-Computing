package com.example.androidstudio.FittnessApp.ui.main.Cardio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.androidstudio.FittnessApp.MainActivity;
import com.example.androidstudio.FittnessApp.R;

import java.util.ArrayList;

public class MyView extends View {

    private static final String TAG = "hsflMyView";
    private Paint myPaint;
    private int heartRatePosY;
    private int time;
    private ArrayList<Integer> heartRateList = new ArrayList<>(); //ArrayList to store the value of the heart sequence
    private ArrayList<Integer> timeList = new ArrayList<>();//ArrayList to store the value of the seconds sequence

    //  background graphic
    private Bitmap bildkorridore;
    private Rect rView;
    private Rect rkorridore;
    // -----

    public MyView(Context context) {
        super(context);
    } // call the Upper class

    //this constructor is called if the GUI element is defined in the XML file
    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.v(TAG, "MyView(Context context, AttributeSet attrs):  ");
        // TODO Auto-generated constructor stub

        // Initialize paint object
        myPaint = new Paint();
        myPaint.setStyle(Paint.Style.FILL);
        myPaint.setTextSize(20);
        myPaint.setStrokeWidth(5);
        myPaint.setColor(Color.RED);


        // Include background graphic
        bildkorridore = BitmapFactory.decodeResource(getResources(), R.drawable.bildmob);
        rView = new Rect();
        rkorridore = new Rect();
        rkorridore.set(0, 0,bildkorridore.getWidth(), bildkorridore.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas)   // Output the graphic here
    {
        super.onDraw(canvas);
        Log.v(TAG, "onDraw():  ");


        rView.set(0, 0, this.getWidth(), this.getHeight());
        canvas.drawBitmap(bildkorridore, rkorridore, rView, myPaint);
        //Pixel coordinates transmitted
        float pixWidth = (float)getWidth();
        float pixHeight = (float)getHeight();
        Log.v(TAG, "    pixWidth: " + pixWidth + "  pixHeight: " + pixHeight );

       //Label from the calories
        canvas.drawText("200 bqm", getWidth()/getWidth(), getHeight()-getHeight()+20, myPaint);
        canvas.drawText("200 bqm", getWidth()/getWidth(), getHeight()-20, myPaint);


          heartRateList.add(heartRatePosY); //add every heartRate to the List
          timeList.add(time); ////add every seconds to the List
         if(heartRateList.size() !=2) {
            // Draw heart rate
         for (int i = 2; i < heartRateList.size(); i++) {
           canvas.drawLine((float)timeList.get((i-1)/4),pixHeight-((float)heartRateList.get(i-1)),(float)timeList.get(i/(4)),(float)heartRateList.get(i), myPaint); // Erste version 1 St
             // canvas.drawLine((float)timeList.get(i-1),pixHeight-((float)heartRateList.get(i-1)),(float)timeList.get(i),pixHeight-((float)heartRateList.get(i)), myPaint);// Zweite version
             //canvas.drawPoint((float)timeList.get((i-1)/4),pixHeight- (float)heartRateList.get((i-1)/4), myPaint); //Dritter version
    }

}
        //Label from the Time
         canvas.drawText("15 Minuten",(float) getWidth()/5, (float)getHeight(),myPaint);

        canvas.drawText("30 Minuten",(float) getWidth()/(float)2.5, (float)getHeight(),myPaint);

        canvas.drawText("45 Minuten",(float) getWidth()/(float)1.6, (float)getHeight(),myPaint);

        canvas.drawText("60 Minuten",(float) getWidth()-100, (float)getHeight(),myPaint);


        Log.v(TAG, "onDraw(canvas).......:   "+ heartRatePosY);
    }
//The values of the heart rate and the time are given and stored to work further with it in the MyView class
    public void setHeartRate(int heartRate, int heartRateZeahler) {
        Log.v(TAG, "setHeartRate();");
        heartRatePosY=heartRate;
        time=heartRateZeahler;
        invalidate(); //redraw


    }
}
