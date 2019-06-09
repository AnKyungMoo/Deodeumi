package mapapi

class FootfallPaths(traffic_icon: String, txt_randmark: String){

    var traffic_icon:String = traffic_icon
    var txt_randmark: String = txt_randmark
    var txt_footfall: String = ""


    constructor(traffic_icon: String, txt_randmark: String, txt_footfall: String): this(traffic_icon, txt_randmark){
        this.txt_footfall = txt_footfall
    }

}
