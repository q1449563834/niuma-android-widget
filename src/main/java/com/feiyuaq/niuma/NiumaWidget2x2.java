package com.feiyuaq.niuma;

/** 2 列 2 行：方卡，状态 + 金额 + 倒计时 + 进度 + 时薪/发薪 */
public class NiumaWidget2x2 extends NiumaWidgetBase {
    @Override
    protected int cols() {
        return 2;
    }

    @Override
    protected int rows() {
        return 2;
    }
}
