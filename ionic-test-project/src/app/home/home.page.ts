import { Component } from '@angular/core';
import {HelloWorld} from '@ionic-native/hello-world/ngx';
declare var window:any;
@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
})
export class HomePage {

  constructor(private helloWorld:HelloWorld) {}

  public nativeToast(){
    this.helloWorld.nativeToast("",1).then(r=>{
      console.log('success');
    }).catch((e)=>{
      console.error(e);
    })
  }

  public withoutWrapper(){
    if(window.cordova && window.cordova.plugins && window.cordova.plugins.HelloWorld){
      window.cordova.plugins.HelloWorld.nativeToast();
    }
  }

}
