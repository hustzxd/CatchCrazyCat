package com.lin.catchcrazycat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Vector;

/**
 * Created by zxd on 2015/7/12.
 */
public class Playground extends SurfaceView implements View.OnTouchListener {

    private static int WIDTH = 40;
    private static final int COL = 10;
    private static final int ROW = 10;
    private static final int BLOCKS = 10;//默认添加的路障数量
    private static final int CAT_X = COL / 2;
    private static final int CAT_Y = ROW / 2;
    private Dot matrix[][];
    private Dot cat;

    public Playground(Context context) {
        super(context);
        getHolder().addCallback(callback);
        matrix = new Dot[ROW][COL];
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                matrix[i][j] = new Dot(j, i);
            }
        }
        setOnTouchListener(this);
        initGame();
    }

    private void redraw() {
        Canvas canvas = getHolder().lockCanvas();
        canvas.drawColor(Color.LTGRAY);
        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < ROW; i++) {
            int offset = 0;
            if (i % 2 != 0) {
                offset = WIDTH / 2;
            }
            for (int j = 0; j < COL; j++) {
                Dot one = getDot(j, i);
                switch (one.getStatus()) {
                    case Dot.STATUS_OFF:
                        paint.setColor(0xFFEEEEEE);
                        break;
                    case Dot.STATUS_ON:
                        paint.setColor(0xFFFFAA00);
                        break;
                    case Dot.STATUS_IN:
                        paint.setColor(0xFFFF0000);
                        break;
                    default:
                        break;
                }
                canvas.drawOval(new RectF(one.getX() * WIDTH + offset, one.getY() * WIDTH,
                        (one.getX() + 1) * WIDTH + offset, (one.getY() + 1) * WIDTH), paint);
            }
        }
        getHolder().unlockCanvasAndPost(canvas);
    }

    Callback callback = new Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            redraw();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            WIDTH = width / (COL + 1);
            redraw();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    private Dot getDot(int x, int y) {
        return matrix[y][x];

    }

    private void initGame() {
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                matrix[i][j].setStatus(Dot.STATUS_OFF);
            }
        }
        cat = new Dot(CAT_X, CAT_Y);
        getDot(CAT_X, CAT_Y).setStatus(Dot.STATUS_IN);
        for (int i = 0; i < BLOCKS; ) {
            int x = (int) (Math.random() * 1000 % COL);
            int y = (int) (Math.random() * 1000 % ROW);

            if (getDot(x, y).getStatus() == Dot.STATUS_OFF) {
                getDot(x, y).setStatus(Dot.STATUS_ON);
                i++;
                System.out.println("Block:" + i);
            }
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
//            Toast.makeText(getContext(), event.getX() + ":" +
//                    event.getY(), Toast.LENGTH_SHORT).show();
            int x, y;
            y = (int) (event.getY() / WIDTH);
            if (y % 2 == 0) {
                x = (int) (event.getX() / WIDTH);
            } else {
                x = (int) ((event.getX() - WIDTH / 2) / WIDTH);
            }

            if (x + 1 > COL || y + 1 > ROW) {
                initGame();
            } else if (getDot(x, y).getStatus() == Dot.STATUS_OFF) {
                getDot(x, y).setStatus(Dot.STATUS_ON);
                move();
            }
            redraw();
        }
        return true;
    }

    private void move() {
        if (isAtEdge(cat)) {
            lose();
            return;
        }
        Vector<Dot> available = new Vector<>();
        Vector<Dot> positive = new Vector<>();
        HashMap<Dot, Integer> al = new HashMap<>();
        for (int i = 1; i < 7; i++) {
            Dot n = getNeighbour(cat, i);
            Log.i("zxd", n.toString());
            if (n.getStatus() == Dot.STATUS_OFF) {
                available.add(n);
                al.put(n, i);
                if (getDistance(n, i) > 0) {
                    positive.add(n);
                }
            }
        }
        if (available.size() == 0) {
            win();
        } else if (available.size() == 1) {
            moveTo(available.get(0));
        } else {
            Dot best = null;
            if (positive.size() != 0) {
                System.out.println("向先进");
                int min = 999;
                for (int i = 0; i < positive.size(); i++) {
                    int a = getDistance(positive.get(i), al.get(positive.get(i)));
                    if (a < min) {
                        min = a;
                        best = positive.get(i);
                    }
                }
                moveTo(best);
            } else {
                System.out.println("多路障");
                int max = 0;
                for (int i = 0; i < available.size(); i++) {
                    int k = getDistance(available.get(i), al.get(available.get(i)));
                    if (k <= max) {
                        max = k;
                        best = available.get(i);
                    }
                }
                moveTo(best);
            }
        }

    }

    private void moveTo(Dot dot) {
        dot.setStatus(Dot.STATUS_IN);
        getDot(cat.getX(), cat.getY()).setStatus(Dot.STATUS_OFF);
        cat.setXY(dot.getX(), dot.getY());
    }

    private int getDistance(Dot one, int i) {
        int distance = 0;
        if (isAtEdge(one)) {
            return 1;
        }
        Dot ori = one, next;
        while (true) {
            next = getNeighbour(ori, i);
            if (next.getStatus() == Dot.STATUS_ON) {
                return distance * -1;
            }
            if (isAtEdge(next)) {
                distance++;
                return distance;
            }
            distance++;
            ori = next;
        }
    }

    private Dot getNeighbour(Dot cat, int i) {
        switch (i) {
            case 1:
                return getDot(cat.getX() - 1, cat.getY());
            case 2:
                if (cat.getY() % 2 == 0) {
                    return getDot(cat.getX() - 1, cat.getY() - 1);
                } else {
                    return getDot(cat.getX(), cat.getY() - 1);
                }
            case 3:
                if (cat.getY() % 2 == 0) {
                    return getDot(cat.getX(), cat.getY() - 1);
                } else {
                    return getDot(cat.getX() + 1, cat.getY() - 1);
                }
            case 4:
                return getDot(cat.getX() + 1, cat.getY());
            case 5:
                if (cat.getY() % 2 == 0) {
                    return getDot(cat.getX(), cat.getY() + 1);
                } else {
                    return getDot(cat.getX() + 1, cat.getY() + 1);
                }
            case 6:
                if (cat.getY() % 2 == 0) {
                    return getDot(cat.getX() - 1, cat.getY() + 1);
                } else {
                    return getDot(cat.getX(), cat.getY() + 1);
                }
            default:
                break;
        }
        return null;
    }

    private void lose() {
        Toast.makeText(getContext(), "Lose", Toast.LENGTH_SHORT).show();
    }

    private void win() {
        Toast.makeText(getContext(), "Win", Toast.LENGTH_SHORT).show();
    }

    private boolean isAtEdge(Dot cat) {
        if (cat.getX() * cat.getY() == 0 || cat.getX() + 1 == COL || cat.getY() + 1 == ROW) {
            return true;
        }
        return false;
    }
}
