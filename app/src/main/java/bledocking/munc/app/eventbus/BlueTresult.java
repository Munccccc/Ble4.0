package bledocking.munc.app.eventbus;

/**
 * Created by munc on 2016/7/27.
 */
public class BlueTresult {

    public BlueTresult(String result) {
        super();
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    //蓝牙回调返回值
    private String result;
}
