package create_3D_model;


import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import okhttp3.*;

import okio.BufferedSink;
import org.json.JSONException;
import org.json.JSONObject;

public class main_okhttp {

    private static JSONObject mainJsonObject = new JSONObject();

    final static String username = "";
    final static String password = "";

    private static String title_project = "T-shirts";

    private static String title_product = "Polo Ralph Lauren Shirt";
    private static String manufacturer = "Shirt"; // название продукта

    private final static String directory_of_images = "/Users/ilyaognev/Desktop/Images_for_Imajize";



    public static void main(String[] args){

        try{

            create_project(new Callback() {
                @Override
                public void call(JSONObject jsonObject) {
                 try{
                     if (jsonObject.has("error")){ gotError(); }
                     if (jsonObject.has("uuid")){
//                         System.out.println(jsonObject.getString("uuid"));
                         mainJsonObject.put("title_project", title_project);
                         mainJsonObject.put("uuid_project", jsonObject.getString("uuid"));
                     }else{
                         System.out.println("No uuid_project in json");
                         return;
                     }
                 } catch (JSONException e){
                     e.printStackTrace();
                 }
                }
            });

            create_product(new Callback() {
                @Override
                public void call(JSONObject jsonObject) {
                    if (jsonObject.has("error")){
                        System.out.println("We got an error");
                        return;
                    }
                  try{
                      if (jsonObject.has("uuid")) {
                          mainJsonObject.put("uuid_product", jsonObject.getString("uuid"));
                      } else {gotError();}
                      if (jsonObject.has("path")) {
                          mainJsonObject.put("path", jsonObject.getString("path"));
                      } else {gotError();}
                      if (jsonObject.has("policy")) {
                          mainJsonObject.put("policy", jsonObject.getString("policy"));
                      } else {gotError();}
                      if (jsonObject.has("signature")) {
                          mainJsonObject.put("signature", jsonObject.getString("signature"));
                      } else {gotError();}
                      if (jsonObject.has("access_key_id")) {
                          mainJsonObject.put("access_key_id", jsonObject.getString("access_key_id"));
                      } else {gotError();}
                      if (jsonObject.has("acl")) {
                          mainJsonObject.put("acl", jsonObject.getString("acl"));
                      } else {gotError();}
                  }catch (JSONException e){
                      e.printStackTrace();
                  }
                }
            });

            upload_images(new Callback() {
                @Override
                public void call(JSONObject jsonObject) {

                }
            });

            finish_upload(new Callback() {
                @Override
                public void call(JSONObject jsonObject) {

                }
            });

        } catch (Exception e){
            e.printStackTrace();
        }
//        System.out.println("JSONObject " + mainJsonObject);
    }


    private static void create_project(Callback callback) throws JSONException, IOException{

        OkHttpClient client = createAuthenticatedClient(username, password);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", title_project)
                .build();

        Request request = new Request.Builder()
                .url("https://app.imajize.com/api/v2/projects")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        JSONObject jsonObject = new JSONObject(response.body().string());
        callback.call(jsonObject);
    }

    private static void create_product(Callback callback) throws JSONException, IOException{

        Calendar calendar = new GregorianCalendar();
        long sku = System.currentTimeMillis();
        mainJsonObject.put("sku", sku);

        OkHttpClient client = createAuthenticatedClient(username, password);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", title_product)
                .addFormDataPart("sku", Long.toString(mainJsonObject.getLong("sku")))
                .addFormDataPart("manufacturer", manufacturer)
                .addFormDataPart("year", Integer.toString(calendar.get(Calendar.YEAR)))
                .addFormDataPart("project", mainJsonObject.getString("uuid_project"))
                .addFormDataPart("images", Integer.toString(new File(directory_of_images).listFiles().length))
                .build();

        Request request = new Request.Builder()
                .url("https://app.imajize.com/api/v2/products")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        JSONObject jsonObject = new JSONObject(response.body().string());

        callback.call(jsonObject);
    }

    private static void upload_images(Callback callback) throws JSONException, IOException{

        File directory = new File(directory_of_images);
        File[] files = directory.listFiles();


        for (int i = 0; i < files.length; i++) {
            if(files[i].toString().endsWith("jpg")){
//                System.out.println("Sending file " + files[i]);
                send_one_image(new Callback() {
                    @Override
                    public void call(JSONObject jsonObject) {

                    }
                }, files[i]);
            }
//            System.out.println("Deleting file " + files[i]);
            files[i].delete();
        }

        callback.call(new JSONObject());

    }

    private static void send_one_image(Callback callback, File file) throws JSONException, IOException{

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", mainJsonObject.getString("path") + file.getName())
                .addFormDataPart("acl", mainJsonObject.getString("acl"))
                .addFormDataPart("AWSAccessKeyId", mainJsonObject.getString("access_key_id"))
                .addFormDataPart("policy", mainJsonObject.getString("policy"))
                .addFormDataPart("signature", mainJsonObject.getString("signature"))
                .addFormDataPart("Content-Type", "image/jpeg")
                .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("image/jpeg"), file))
                .build();

        Request request = new Request.Builder()
                .url("http://s3.amazonaws.com/imajize-production-source")
                .post(formBody)
                .build();

        client.newCall(request).execute();
        callback.call(new JSONObject());
    }

    private static void finish_upload(Callback callback) throws JSONException, IOException{

        OkHttpClient client = createAuthenticatedClient(username, password);
        String URL = "https://app.imajize.com/api/v2/products/" + mainJsonObject.getString("uuid_product") + "/uploaded";

        Request request = new Request.Builder()
                .url(URL)
                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return null;
                    }

                    @Override
                    public void writeTo(BufferedSink bufferedSink) throws IOException {

                    }
                })
                .build();

        client.newCall(request).execute();
        callback.call(new JSONObject());
    }


    private static OkHttpClient createAuthenticatedClient(final String username, final String password) {
        OkHttpClient httpClient = new OkHttpClient.Builder().authenticator(new Authenticator() {
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder().header("Authorization", credential).build();
            }
        }).build();
        return httpClient;
    }

    private static void gotError(){
        System.out.println("We got an error");
        System.exit(0);
    }

    public interface Callback {
        void call(JSONObject jsonObject);
    }

}
