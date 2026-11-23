package app.neurolingua.mobile;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private FrameLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        root.setBackgroundColor(getColor(R.color.paper));
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets space = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(space.left, space.top, space.right, space.bottom);
                return insets;
            });
        }
        setContentView(root);
        TextView name = new TextView(this);
        name.setText(R.string.app_name);
        name.setTextSize(30);
        name.setGravity(Gravity.CENTER);
        root.addView(name);
    }
}
