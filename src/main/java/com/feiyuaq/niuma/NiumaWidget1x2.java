package com.feiyuaq.niuma;

/** 2 列 1 行：桌面上的迷你条，只有状态 + 金额 + 进度 */
public class NiumaWidget1x2 extends NiumaWidgetBase {
    @Override
    protected int cols() {
        return 2;
    }

    @Override
    protected int rows() {
        return 1;
    }
}
