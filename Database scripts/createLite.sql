create table users
(
    uid      integer autoincrement primary key,
    password text not null
);

create table threads
(
    tid   integer autoincrement primary key,
    tname text not null
);

create table messages
(
    tid    integer not null,
    uid    integer not null,
    m_cont text    null,
    m_time text    not null,
    primary key (tid, uid, m_time),
    foreign key (tid) references threads (tid),
    foreign key (uid) references users (uid)
);
