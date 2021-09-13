Zuul还会提供Hystrix的处理，在其内部提供一个接口FallbackProvider 

这里的getRoute方法的作用是做匹配，指定对什么微服务执行降级。fallbackResponse方法是降级方法的逻辑。使用Hystrix降级的接口定义一直在变化，旧的Spring Cloud版本之间会存在比较大的差异，这是大家需要注意的



```java
@Component
public class UserFallBack implements FallbackProvider {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 指定那些微服务需要进行降级。 如果需要指定特定的微服务，可以返回具体的service id
     *
     * @return
     */
    @Override
    public String getRoute() {
        return "*";
    }

    /**
     * 降级方法
     *
     * @param route
     * @param cause
     * @return
     */
    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {

        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return HttpStatus.INTERNAL_SERVER_ERROR.value();
            }

            @Override
            public String getStatusText() throws IOException {
                return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() throws IOException {
                logger.info("error [{}]", cause.getCause().getMessage());
                String message = cause.getCause().getMessage();
                return new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
                return httpHeaders;
            }
        };
    }
}

```

