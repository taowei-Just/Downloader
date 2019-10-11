package com.tao.downloader;

import java.util.List;

public class MQGoodsSync {
 
    List<Data> data;

    public static class Data {
//    "goodName":"广州双喜(软)","img":"http://dc.sun-hyt.com/uploads/img/2017-09-14/6901028001687.png","num":14,"retailPrice":14.00,"unit":"1"
        String goodName;// String 商品名称String ,
        String goodCode;// String 商品编码String ,
        String img;// String 商品图片地址String ,
        String num;// String 商品库存数据String ,
        String unit;// String 商品单位String ,   2/1条/包
        String retailPrice;// String 零售价String
        int status =0;   // 0 可用 ，1 不可用

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getGoodName() {
            return goodName;
        }

        public void setGoodName(String goodName) {
            this.goodName = goodName;
        }

        public String getGoodCode() {
            return goodCode;
        }

        public void setGoodCode(String goodCode) {
            this.goodCode = goodCode;
        }

        public String getImg() {
            return img;
        }

        public void setImg(String img) {
            this.img = img;
        }

        public String getNum() {
            return num;
        }

        public void setNum(String num) {
            this.num = num;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getRetailPrice() {
            return retailPrice;
        }

        public void setRetailPrice(String retailPrice) {
            this.retailPrice = retailPrice;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "goodName='" + goodName + '\'' +
                    ", code='" + goodCode + '\'' +
                    ", img='" + img + '\'' +
                    ", num='" + num + '\'' +
                    ", unit='" + unit + '\'' +
                    ", retailPrice='" + retailPrice + '\'' +
                    '}';
        }
    }

 

    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;

    }

    @Override
    public String toString() {
        return "MQGoodsSync{" +
                ", data=" + data +
                '}';
    }
}
