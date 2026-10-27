from pathlib import Path
from subprocess import run
root=Path(__file__).resolve().parents[2]
java=list(root.glob('*/src/main/java/**/*.java'))
assert len(java)>=66, f'expected >=66 Java files, got {len(java)}'
controller_files=[p for p in java if 'controller' in p.parts]
for p in controller_files:
    txt=p.read_text()
    if '@RequestMapping(' in txt:
        assert ('/api/v1' in txt or ('@RequestMapping("/api")' in txt and '/v1/' in txt)), f'API version changed: {p}'
booking=(root/'booking-service/src/main/java/com/cinema/booking/service/TicketService.java').read_text()
assert 'saveAndFlush(new SeatClaim' in booking
assert 'RELEASE_SCRIPT' in booking and "redis.call('get'" in booking
assert 'getRoomId()' in booking and 'isShowtimeBookable' in booking
assert 'getLockToken()' in booking and 'findByIdForUpdate' in booking
assert 'expirePendingTickets' in booking and '@Scheduled' in booking
assert 'payTicket(' in booking and 'cancelTicket(' in booking
assert 'new ConflictException' in booking
assert not any('jwt.secret=${JWT_SECRET:' in p.read_text() for p in root.glob('*/src/main/resources/application.properties'))
assert '123456' not in (root/'user-service/src/main/resources/application.properties').read_text()
print(f'PASS source checks: {len(java)} Java files; {len(controller_files)} controller files; API paths /api/v1 preserved')
