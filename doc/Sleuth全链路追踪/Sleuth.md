使用链路追踪，使得业务请求能够被追踪，在发生异常时可以快速准确地定位，这样开发者才能快速进行处理

对于全链路追踪组件，Dapper论文还提出了3点要求

1. 低消耗：指在分布式系统中，植入分布式链路追踪组件，对系统性能的损耗应该是很小的
2. 应用级的透明：指在植入分布式链路追踪组件后，对原有的业务应该是透明的，不应该影响原有代码的编写和业务，链路追踪组件会按照自己的维度去采集服务调用之间的数据，并且通过日志进行展示
3. 延展性：链路追踪组件应该能够进行扩展，以适应分布式系统不断膨胀和转变的需求



全链路追踪组件来说，它采集了许多数据，那么这些数据又要遵循什么规则呢？这就涉及相关的术语了，其说明如下

1. span：基本单元。例如，执行一次服务调用就生成一个span，用来记录当时的情况，它会以一个64位ID作为唯一标识。span还有其他数据信息，如摘要、时间戳事件、关键值注释（tags）、span的ID和进度ID（通常是IP地址）
2. trace：它代表一次请求，会以一个64位ID作为唯一标识，可以将它理解为一个业务号，通过它的ID标识多个span为同一个业务请求。它会以树状的形式展示服务调用，在树状中可以看到它调用多个span的轨迹
3. annotation：注解，它代表服务调用的客户端和服务端的行为，存在以下注解
   1. cs（Client Sent）：客户端（服务消费者）发起一个服务调用，它意味着一个span的开始
   2. sr（Server Received）：服务端（服务提供者）获得请求信息，并开始处理。将sr减去cs得到的时间戳，就是网络延迟时间
   3. ss（Server Sent）：服务端处理完请求，将结果返回给客户端。将ss减去sr得到的时间戳，就是服务端处理请求所用的时间
   4. cr（Client Received）：它代表一个span的结束，客户端成功接收到服务端的回复。将cr减去cs得到的时间戳，就是客户端从服务端获取响应所用的时间
4. 

