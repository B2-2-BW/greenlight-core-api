//import brave.Tracing;
//import brave.handler.SpanHandler;
//import brave.propagation.B3Propagation;
//import brave.propagation.CurrentTraceContext;
//import brave.propagation.TraceContext;
//import io.micrometer.tracing.exporter.SpanReporter;
//import io.micrometer.tracing.exporter.ZipkinSpanExporter;
//import io.micrometer.tracing.brave.bridge.BraveTracer;
//import zipkin2.reporter.AsyncReporter;
//import zipkin2.reporter.Sender;
//import zipkin2.reporter.okhttp3.OkHttpSender;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class TracingConfig {
//
//    // Zipkin으로 전송할 Sender 설정 (OkHttp 사용)
//    @Bean
//    public Sender zipkinSender() {
//        return OkHttpSender.create("http://localhost:9411/api/v2/spans");
//    }
//
//    // ZipkinSpanExporter 설정 (SpanHandler 사용)
//    @Bean
//    public SpanHandler zipkinSpanHandler(Sender sender) {
//        return ZipkinSpanExporter.create(sender);
//    }
//
//    // Brave Tracing 설정
//    @Bean
//    public Tracing braveTracing(SpanHandler zipkinSpanHandler) {
//        return Tracing.newBuilder()
//                .currentTraceContext(CurrentTraceContext.Default.create())
//                .propagationFactory(B3Propagation.newFactoryBuilder().build()) // B3 트레이싱 사용
//                .addSpanHandler(zipkinSpanHandler)
//                .build();
//    }
//
//    // Micrometer Tracer 설정
//    @Bean
//    public io.micrometer.tracing.Tracer micrometerTracer(Tracing tracing) {
//        return new BraveTracer(tracing.tracer(), tracing.currentTraceContext());
//    }
//}