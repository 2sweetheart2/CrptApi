import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CrptApi {

    /**
     *   Map предназначен для конвертации TimeUnit в nano
     */
    private static final Map<TimeUnit, Long> TIME_UNIT_MAP = new HashMap<>();
    static {
        TIME_UNIT_MAP.put(TimeUnit.NANOSECONDS, 1L);
        TIME_UNIT_MAP.put(TimeUnit.MICROSECONDS, 1000L);
        TIME_UNIT_MAP.put(TimeUnit.MILLISECONDS, 1000L * 1000L);
        TIME_UNIT_MAP.put(TimeUnit.SECONDS, 1000L * 1000L * 1000L);
        TIME_UNIT_MAP.put(TimeUnit.MINUTES, 60L * 1000L * 1000L * 1000L);
        TIME_UNIT_MAP.put(TimeUnit.HOURS, 60L * 60L * 1000L * 1000L * 1000L);
        TIME_UNIT_MAP.put(TimeUnit.DAYS, 24L * 60L * 60L * 1000L * 1000L * 1000L);
    }


    private final TimeUnit timeUnit;
    private final int requestLimit;

    /**
     *  Счётчики времени и кол-во отправленных запросов для правильной логики работы
     */
    private final AtomicLong timer = new AtomicLong();
    private final AtomicInteger counter = new AtomicInteger();

    /**
     *   End-point для URL для запроса
     */
    private final String END_POINT = "https://ismp.crpt.ru/api/v3/lk/documents/create";


    /**
     * Конструктор класса
     * @param timeUnit указание промежуток времени – секунда, минута и пр.
     * @param requestLimit положительное значение, которое определяет максимальное количество запросов в этом промежутке времени
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    /**
     * Функция преобразование TimeUnit в nano
     * @return nano time
     */
    private Long unitToNano() {
        return TIME_UNIT_MAP.getOrDefault(timeUnit, 1L);
    }

    /**
     * Функция для проверки, прошло ли нужное время с последнего запроса
     * @return не прошло ли нужное время с прошлого запроса
     *
     */
    private boolean checkTime() {
        long dev = System.nanoTime() - timer.get();
        if (dev <= unitToNano())
            return true;
        timer.addAndGet(dev);
        return false;
    }

    /**
     * Функция для проверки превышения лимита запросов
     * @return превышен ли лимит запросов
     */
    private boolean checkCount() {
        return counter.addAndGet(1) >= requestLimit;
    }

    /**
     * Метод для отправки запроса (сокращение для двух одинаковых)
     * @param doc объект документа
     * @param sign подпись (Я НЕ ЗНАЮ ЧТО С НЕЙ ДЕЛАТЬ ИБО В ТЗ НИЧЕГО ПРО НЕЁ НЕ СКАЗАЛИ)
     * @param callBack callback для ответа сервера
     * @see ICallBack
     */
    private void doSend(Object doc, String sign, ICallBack callBack){
        if (checkTime()) {
            if (checkCount()) {
                return;
            }
        } else
            counter.set(0);
        sendRequest(doc,callBack);
    }

    /**
     * Метод для отправки запроса по end-pointУ без получения ответа от сервера
     * @param doc объект документа
     * @param sign подпись (Я НЕ ЗНАЮ ЧТО С НЕЙ ДЕЛАТЬ ИБО В ТЗ НИЧЕГО ПРО НЕЁ НЕ СКАЗАЛИ)
     */
    public void send(Object doc, String sign) {
        doSend(doc,sign,(res)->{});
    }

    /**
     * Метод для отправки запроса по end-pointУ с получения ответа от сервера
     * @param doc объект документа
     * @param sign подпись (Я НЕ ЗНАЮ ЧТО С НЕЙ ДЕЛАТЬ ИБО В ТЗ НИЧЕГО ПРО НЕЁ НЕ СКАЗАЛИ)
     * @param callBack callback для ответа с сервера
     * @see ICallBack
     */
    public void sendWithCallBack(Object doc, String sign, ICallBack callBack) {
        doSend(doc,sign,callBack);
    }

    /**
     * Метод для отправки запроса по end-pointУ
     * @param object объект документа
     * @param callBack callback для ответа с сервера
     * @see ICallBack
     * @see Gson
     * @see HttpClient
     */
    private void sendRequest(Object object, ICallBack callBack) {
        try {
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String jsonData = gson.toJson(object);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(END_POINT))
                    .method("POST", HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();
            try {
                HttpResponse<String> reponse;
                reponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                callBack.result(reponse.body());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                callBack.result(null);
            }
        }catch (URISyntaxException e) {
            e.printStackTrace();
            callBack.result(null);
        }
    }


    /**
     * класс интерфейса для передачи ответа от сервера
     */
    public interface ICallBack {
        void result(String data);
    }
}
