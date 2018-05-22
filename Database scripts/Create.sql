create table users
(
  uid      int auto_increment primary key,
  password varchar(16) not null
);

create table threads
(
  tid   int auto_increment
    primary key,
  tname varchar(15) null
);

create table messages
(
  tid    int          not null,
  uid    int          not null,
  m_cont varchar(200) null,
  m_time datetime     not null,
  primary key (tid, uid, m_time),
  foreign key (tid) references threads (tid),
  foreign key (uid) references users (uid)
);
