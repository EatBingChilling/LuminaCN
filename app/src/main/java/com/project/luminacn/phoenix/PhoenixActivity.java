package com.project.luminacn.phoenix;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class PhoenixActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phoenix);

        // 1. 初始化 Fragment 列表
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new HomeFragment());      // 对应 tab_home
        fragments.add(new DashboardFragment()); // 对应 tab_dashboard
        fragments.add(new ProfileFragment());   // 对应 tab_profile

        // 2. 设置 ViewPager2 Adapter
        ViewPager2 viewPager2 = findViewById(R.id.viewPager2);
        viewPager2.setAdapter(new ViewPagerAdapter(this, fragments));

        // 3. 绑定 BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // 4. BottomNav 点击切换 ViewPager2 页面
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.tab_home) {
                viewPager2.setCurrentItem(0, true);
            } else if (item.getItemId() == R.id.tab_dashboard) {
                viewPager2.setCurrentItem(1, true);
            } else if (item.getItemId() == R.id.tab_profile) {
                viewPager2.setCurrentItem(2, true);
            }
            return true;
        });

        // 5. ViewPager2 滑动时同步更新 BottomNav 选中项
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.tab_home);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.tab_dashboard);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.tab_profile);
                        break;
                }
            }
        });
    }
}
