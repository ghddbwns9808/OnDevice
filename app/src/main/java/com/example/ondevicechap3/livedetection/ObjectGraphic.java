package com.example.ondevicechap3.livedetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.DetectedObject.Label;
import java.util.Locale;

/** Draw the detected object info in preview. */
public class ObjectGraphic extends GraphicOverlay.Graphic {

    private static final float TEXT_SIZE = 54.0f;
    private static final float STROKE_WIDTH = 4.0f;
    private static final int NUM_COLORS = 10;
    private static final int[][] COLORS =
            new int[][] {
                    // {Text color, background color}
                    {Color.BLACK, Color.WHITE},
                    {Color.WHITE, Color.MAGENTA},
                    {Color.BLACK, Color.LTGRAY},
                    {Color.WHITE, Color.RED},
                    {Color.WHITE, Color.BLUE},
                    {Color.WHITE, Color.DKGRAY},
                    {Color.BLACK, Color.CYAN},
                    {Color.BLACK, Color.YELLOW},
                    {Color.WHITE, Color.BLACK},
                    {Color.BLACK, Color.GREEN}
            };
    private static final String LABEL_FORMAT = "%.2f%% confidence (index: %d)";

    private final DetectedObject object;
    private final Paint[] boxPaints;
    private final Paint[] textPaints;
    private final Paint[] labelPaints;

    ObjectGraphic(GraphicOverlay overlay, DetectedObject object) {
        super(overlay);

        this.object = object;

        int numColors = COLORS.length;
        textPaints = new Paint[numColors];
        boxPaints = new Paint[numColors];
        labelPaints = new Paint[numColors];
        for (int i = 0; i < numColors; i++) {
            textPaints[i] = new Paint();
            textPaints[i].setColor(COLORS[i][0] /* text color */);
            textPaints[i].setTextSize(TEXT_SIZE);

            boxPaints[i] = new Paint();
            boxPaints[i].setColor(COLORS[i][1] /* background color */);
            boxPaints[i].setStyle(Paint.Style.STROKE);
            boxPaints[i].setStrokeWidth(STROKE_WIDTH);

            labelPaints[i] = new Paint();
            labelPaints[i].setColor(COLORS[i][1] /* background color */);
            labelPaints[i].setStyle(Paint.Style.FILL);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        // Decide color based on object tracking ID
        int colorID =
                object.getTrackingId() == null ? 0 : Math.abs(object.getTrackingId() % NUM_COLORS);
        float textWidth = textPaints[colorID].measureText("Tracking ID: " + object.getTrackingId());
        float lineHeight = TEXT_SIZE + STROKE_WIDTH;
        float yLabelOffset = -lineHeight;

        // Calculate width and height of label box
        for (Label label : object.getLabels()) {
            textWidth = Math.max(textWidth, textPaints[colorID].measureText(label.getText()));
            textWidth =
                    Math.max(
                            textWidth,
                            textPaints[colorID].measureText(
                                    String.format(
                                            Locale.US, LABEL_FORMAT, label.getConfidence() * 100, label.getIndex())));
            yLabelOffset -= 2 * lineHeight;
        }

        // Draws the bounding box.
        RectF rect = new RectF(object.getBoundingBox());
        // If the image is flipped, the left will be translated to right, and the right to left.
        float x0 = translateX(rect.left);
        float x1 = translateX(rect.right);
        rect.left = Math.min(x0, x1);
        rect.right = Math.max(x0, x1);
        rect.top = translateY(rect.top);
        rect.bottom = translateY(rect.bottom);
        canvas.drawRect(rect, boxPaints[colorID]);

        // Draws other object info.
        canvas.drawRect(
                rect.left - STROKE_WIDTH,
                rect.top + yLabelOffset,
                rect.left + textWidth + (2 * STROKE_WIDTH),
                rect.top,
                labelPaints[colorID]);
        yLabelOffset += TEXT_SIZE;
        canvas.drawText(
                "Tracking ID: " + object.getTrackingId(),
                rect.left,
                rect.top + yLabelOffset,
                textPaints[colorID]);
        yLabelOffset += lineHeight;

        for (Label label : object.getLabels()) {
            canvas.drawText(label.getText(), rect.left, rect.top + yLabelOffset, textPaints[colorID]);
            yLabelOffset += lineHeight;
            canvas.drawText(
                    String.format(Locale.US, LABEL_FORMAT, label.getConfidence() * 100, label.getIndex()),
                    rect.left,
                    rect.top + yLabelOffset,
                    textPaints[colorID]);

            yLabelOffset += lineHeight;
        }
    }
}